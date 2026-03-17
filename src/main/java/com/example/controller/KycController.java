package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.api.KycApi;
import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
import com.example.security.CustomUserPrincipal;
import com.example.service.KycService;

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

        ApiResponse<String> response =
                kycService.uploadKyc(
                        principal.getCustomerId(),
                        documentType,
                        documentNumber,
                        file
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ApiResponse<KycResponse>> getStatus(
            CustomUserPrincipal principal,KycStatus status) {

        ApiResponse<KycResponse> response =
                kycService.getKycStatus(principal.getCustomerId());

        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<ApiResponse<String>> verify(
            UUID kycId,
            CustomUserPrincipal principal,
            KycVerifyRequest request) {

        ApiResponse<String> response =
                kycService.verifyKyc(
                        kycId,
                        principal.getUserId(),
                        request
                );

        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<ApiResponse<List<KycResponse>>> pending() {

        ApiResponse<List<KycResponse>> response =
                kycService.getPendingKyc();

        return ResponseEntity.ok(response);
    }
}