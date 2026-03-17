package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "KYC Management", description = "KYC submission and verification APIs")
@RequestMapping("/api/v1/kyc")
public interface KycApi {

    // =====================================================
    // CUSTOMER SUBMIT / RESUBMIT
    // =====================================================
    @Operation(summary = "Upload or resubmit KYC document")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "KYC submitted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<String>> uploadKyc(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam String documentType,
            @RequestParam String documentNumber,
            @RequestPart MultipartFile file
    );

    // =====================================================
    // CUSTOMER STATUS
    // =====================================================
    @Operation(summary = "Get logged-in customer KYC status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KYC status fetched",
                    content = @Content(
                            schema = @Schema(implementation = KycResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @GetMapping("/status")
    ResponseEntity<ApiResponse<KycResponse>> getStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            KycStatus status
    );

    // =====================================================
    // ADMIN VERIFY
    // =====================================================
    @Operation(summary = "Admin verify or reject KYC")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KYC verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "KYC record not found"
            )
    })
    @PutMapping("/{kycId}")
    ResponseEntity<ApiResponse<String>> verify(
            @PathVariable UUID kycId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody KycVerifyRequest request
    );

    // =====================================================
    // ADMIN PENDING LIST
    // =====================================================
    @Operation(summary = "Get all pending KYC records (Admin)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pending KYC list fetched"
            )
    })
    @GetMapping("/pending")
    ResponseEntity<ApiResponse<List<KycResponse>>> pending();
}