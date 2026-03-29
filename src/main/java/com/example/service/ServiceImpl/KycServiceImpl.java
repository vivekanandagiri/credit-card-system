package com.example.service.ServiceImpl;

import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.KycResponse;

import com.example.entity.Customer;
import com.example.entity.KycRecord;

import com.example.enums.KycStatus;

import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.KycMapper;
import com.example.repository.KycRepository;
import com.example.service.CustomerService;
import com.example.service.KycService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link KycService}.
 *
 * <p><strong>Domain ownership:</strong> KycService owns {@code KycRepository}.
 * Customer lookups go through {@link CustomerService} — not the customer repository directly.
 */
@Service
@Transactional
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final CustomerService customerService;
    private final KycMapper kycMapper;

    public KycServiceImpl(KycRepository kycRepository,
                          CustomerService customerService, KycMapper kycMapper) {
        this.kycRepository = kycRepository;
        this.customerService = customerService;
		this.kycMapper = kycMapper;
    }


    /**
     * Submits or re-submits a KYC document.
     * Deactivates any previous records before saving the new one.
     *
     * @throws BusinessRuleException if the existing KYC is SUBMITTED or VERIFIED
     */
    @Override
    @Transactional // Explicit write transaction
    public String uploadKyc(UUID customerId, String documentType, String documentNumber, MultipartFile file) {

        Customer customer = customerService.getCustomer(customerId);

        validateNoBlockingKycRecord(customerId);
        
        // Relies on JPA dirty checking to save the deactivated records automatically.
        deactivatePreviousKycRecords(customerId);

        KycRecord kyc = buildKycRecord(customer, documentType, documentNumber, file);
        kycRepository.save(kyc);

        return kyc.getStatus().name();
    }

    /**
     * (Admin) Updates the status of a KYC record.
     *
     * @throws BusinessRuleException if the KYC is already finalized or rejection reason is missing
     */
    @Override
    @Transactional // Explicit write transaction
    public String updateKycStatus(UUID kycId, UUID adminId, KycStatusUpdateRequest request) {

        KycRecord kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record not found"));

        if (kyc.getStatus() == KycStatus.VERIFIED || kyc.getStatus() == KycStatus.REJECTED) {
            throw new BusinessRuleException("KYC is already finalized and cannot be updated");
        }
        
        if (request.getStatus() == KycStatus.REJECTED && 
           (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new BusinessRuleException("Compliance requires a rejection reason when declining a KYC record");
        }

        applyKycDecision(kyc, request, adminId);
        
        // kycRepository.save(kyc) is omitted here because @Transactional ensures 
        // the dirty-checked entity is automatically flushed to the DB.

        return kyc.getStatus().name();
    }

	/** Returns the active KYC record for the customer. */
    @Override
    public KycResponse getKycStatus(UUID customerId) {
        KycRecord kyc = kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active KYC record found"));
        return kycMapper.toResponse(kyc);
    }

    /** (Admin) Returns all SUBMITTED KYC records pending review. */
    @Override
    public List<KycResponse> getPendingKyc() {
        List<KycResponse> pending = kycRepository.findByStatus(KycStatus.SUBMITTED)
                .stream()
                .map(kycMapper::toResponse)
                .toList();
        return pending;
    }


    /**
     * {@inheritDoc}
     *
     * <p>Used by {@link com.example.service.CreditAccountApplicationService} to
     * enforce the KYC gate without accessing the KYC repository directly.
     */
    @Override
    public boolean isKycVerified(UUID customerId) {
        return kycRepository.existsByCustomerCustomerIdAndStatus(customerId, KycStatus.VERIFIED);
    }
	
	//----------------Private Helpers-------------
    private void validateNoBlockingKycRecord(UUID customerId) {
        kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId).ifPresent(existing -> {
            if (existing.getStatus() == KycStatus.SUBMITTED) {
                throw new BusinessRuleException("KYC already submitted and is under review");
            }
            if (existing.getStatus() == KycStatus.VERIFIED) {
                throw new BusinessRuleException("KYC already verified. Re-upload is not allowed");
            }
        });
    }
 
    private void deactivatePreviousKycRecords(UUID customerId) {
        kycRepository.findByCustomerCustomerId(customerId)
                .forEach(record -> record.setActive(false));
    }
 
    private KycRecord buildKycRecord(
            Customer customer, String documentType, String documentNumber, MultipartFile file) {
        try {
            return kycMapper.toEntity(customer, documentType, documentNumber, file);
        } catch (IOException e) {
        	// Contextual logging is critical for file I/O operations
            throw new BusinessRuleException("Failed to process KYC document: " + e.getMessage());
        }
    }
 
    private void applyKycDecision(KycRecord kyc, KycStatusUpdateRequest request, UUID adminId) {
        kyc.setStatus(request.getStatus());
        
        if (request.getStatus() == KycStatus.REJECTED || request.getStatus() == KycStatus.RESUBMIT_REQUIRED) {
            kyc.setRejectionReason(request.getRejectionReason());
        } else {
            kyc.setRejectionReason(null);
        }
        
        // Audit trail
        kyc.setVerifiedAt(Instant.now());
        kyc.setVerifiedBy(adminId);
    }
}