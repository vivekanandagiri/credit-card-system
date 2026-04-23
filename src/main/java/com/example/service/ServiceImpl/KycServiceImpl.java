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
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link KycService} responsible for managing
 * KYC (Know Your Customer) life-cycle.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *     <li>Submit and re-submit KYC documents</li>
 *     <li>Handle admin verification decisions</li>
 *     <li>Maintain KYC audit trail</li>
 * </ul>
 *
 * <p><b>KYC Life-cycle:</b></p>
 * <ul>
 *     <li>SUBMITTED → Under review</li>
 *     <li>VERIFIED → Approved</li>
 *     <li>REJECTED → Failed with reason</li>
 *     <li>RESUBMIT_REQUIRED → Needs correction</li>
 * </ul>
 *
 * <p><b>Design Notes:</b></p>
 * <ul>
 *     <li>KYC records are versioned (only one active at a time)</li>
 *     <li>Previous records are deactivated on new submission</li>
 *     <li>Customer lookup is delegated to {@link CustomerService}</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final CustomerService customerService;
    private final KycMapper kycMapper;

    /**
     * Uploads or re-submits a KYC document.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>Cannot submit if existing KYC is SUBMITTED or VERIFIED</li>
     *     <li>Previous KYC records are deactivated</li>
     * </ul>
     *
     * @param customerId     customer ID
     * @param documentType   document type (PAN, Aadhaar, etc.)
     * @param documentNumber document number
     * @param file           uploaded document
     * @return current KYC status
     */
    @Override
    @Transactional 
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
     * Admin action to update KYC status.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>Cannot update VERIFIED or REJECTED records</li>
     *     <li>Rejection must include a reason</li>
     * </ul>
     *
     * @param kycId   KYC record ID
     * @param adminId admin performing action
     * @param request update payload
     * @return updated KYC status
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

        kycRepository.save(kyc);

        return kyc.getStatus().name();

    }

    /**
     * Returns active KYC record for customer.
     */
    @Override
    public KycResponse getKycStatus(UUID customerId) {
        KycRecord kyc = kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active KYC record found"));
        return kycMapper.toResponse(kyc);
    }


    /**
     * Returns all pending KYC records for admin review.
     */
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
     * /**
     * Checks whether customer KYC is verified.
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