package com.example.service.ServiceImpl;

import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;

import com.example.entity.Customer;
import com.example.entity.KycRecord;

import com.example.enums.KycStatus;

import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.KycMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.KycRepository;

import com.example.service.KycService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final CustomerRepository customerRepository;
    private final KycMapper kycMapper;

    public KycServiceImpl(KycRepository kycRepository,
                          CustomerRepository customerRepository, KycMapper kycMapper) {
        this.kycRepository = kycRepository;
        this.customerRepository = customerRepository;
		this.kycMapper = kycMapper;
    }


    // CUSTOMER UPLOAD / RESUBMIT
    @Override
    public ApiResponse<String> uploadKyc(
            UUID customerId,
            String documentType,
            String documentNumber,
            MultipartFile file) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found"));

        kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId)
                .ifPresent(existing -> {

                    if (existing.getStatus() == KycStatus.SUBMITTED) {
                        throw new BusinessRuleException(
                                "KYC already submitted and under review");
                    }

                    if (existing.getStatus() == KycStatus.VERIFIED) {
                        throw new BusinessRuleException(
                                "KYC already verified. Re-upload not allowed");
                    }
                });

        // deactivate previous versions
        kycRepository.findByCustomerCustomerId(customerId)
                .forEach(k -> k.setActive(false));


        KycRecord kyc;

        try {
            kyc = kycMapper.toEntity(customer, documentType, documentNumber, file);
        } catch (IOException e) {
            throw new BusinessRuleException("File upload failed");
        }
        
        kycRepository.save(kyc);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.CREATED.value(),
                "KYC submitted successfully",
                kyc.getStatus().name()
        );
    }


    // ADMIN VERIFY

    @Override
    public ApiResponse<String> verifyKyc(
            UUID kycId,
            UUID adminId,
            KycVerifyRequest request) {

        KycRecord kyc = kycRepository.findById(kycId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("KYC not found"));

        if (kyc.getStatus() == KycStatus.VERIFIED) {
            throw new BusinessRuleException("KYC already verified");
        }

        if (request.isApproved()) {
            kyc.setStatus(KycStatus.VERIFIED);
            kyc.setRejectionReason(null);
        } else {
            kyc.setStatus(KycStatus.RESUBMIT_REQUIRED);
            kyc.setRejectionReason(request.getRejectionReason());
        }

        kyc.setVerifiedAt(Instant.now());
        kyc.setVerifiedBy(adminId);


        kycRepository.save(kyc);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "KYC verification processed",
                kyc.getStatus().name()
        );
    }

    // GET STATUS (CUSTOMER)

    @Override
    public ApiResponse<KycResponse> getKycStatus(UUID customerId) {

        KycRecord kyc = kycRepository
                .findByCustomerCustomerIdAndIsActiveTrue(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("No active KYC found"));

        KycResponse response = kycMapper.toResponse(kyc);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "KYC status fetched successfully",
                response
        );
    }

    // ADMIN LIST PENDING
    @Override
    public ApiResponse<List<KycResponse>> getPendingKyc() {

        List<KycRecord> records =
                kycRepository.findByStatus(KycStatus.SUBMITTED);

        List<KycResponse> pending = records.stream()
                .map(kycMapper::toResponse)
                .toList();
        
        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Pending KYC list fetched",
                pending
        );
    }
}