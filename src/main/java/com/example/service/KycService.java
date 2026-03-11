package com.example.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;

public interface KycService {

    ApiResponse<String> uploadKyc(
            UUID customerId,
            String documentType,
            String documentNumber,
            MultipartFile file
    );

    ApiResponse<String> verifyKyc(
            UUID kycId,
            UUID adminUserId,
            KycVerifyRequest request
    );

    ApiResponse<KycResponse> getKycStatus(UUID customerId);

    ApiResponse<List<KycResponse>> getPendingKyc();
}