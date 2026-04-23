package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.api.KycApi;
import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
import com.example.exception.BadRequestException;
import com.example.security.CustomUserPrincipal;
import com.example.service.KycService;

/**
 * REST endpoint for Identity Verification routing.
 * Delegates all business and compliance rules to the KycService.
 */
@RestController
public class KycController implements KycApi {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @Override
    public ResponseEntity<ApiResponse<String>> uploadKyc(
            CustomUserPrincipal principal,
            String documentType,
            String documentNumber,
            MultipartFile file) {

    	validateFile(file);
        // IDOR Protection: Forcing the customerId from the token context
        String result = kycService.uploadKyc(
                principal.getCustomerId(),
                documentType,
                documentNumber,
                file
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                		HttpStatus.CREATED,
                		"KYC submitted successfully",
                		result));
    }

    //Helper method to handle big file 
    private void validateFile(MultipartFile file) {
    	if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
    	long maxSize = 5 * 1024 * 1024;
    	if (file.getSize() > maxSize) {
            throw new BadRequestException("File size must not exceed 5MB");
        }
    	
        String contentType = file.getContentType();

        if (contentType == null ||
            (!contentType.equals("application/pdf") &&
             !contentType.equals("image/jpeg") &&
             !contentType.equals("image/png"))) {

            throw new BadRequestException("Only PDF, JPG, PNG files are allowed");
        }
		
	}

	@Override
    public ResponseEntity<ApiResponse<KycResponse>> getStatus(
            CustomUserPrincipal principal, KycStatus status) {

        KycResponse result = kycService.getKycStatus(principal.getCustomerId());

        return ResponseEntity.ok(ApiResponse.success(
        		HttpStatus.OK,
        		"KYC status fetched successfully",
        		result));
    }

    // SECURITY: strictly gated to Back-Office personnel. 
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<ApiResponse<String>> verify(
            UUID kycId,
            CustomUserPrincipal principal,
            @RequestBody KycStatusUpdateRequest request) {

        // The admin's UUID is passed down to maintain the compliance audit trail (who approved what)
        String result = kycService.updateKycStatus(kycId, principal.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success(
        		HttpStatus.OK,
        		"KYC status updated successfully", result));
    }

    // SECURITY: strictly gated to Back-Office personnel. 
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<ApiResponse<List<KycResponse>>> pending() {

        List<KycResponse> result = kycService.getPendingKyc();

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Pending KYC records fetched successfully",
                		result)
        );
    }
}