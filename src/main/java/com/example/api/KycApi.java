package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for KYC (Know Your Customer) management.
 *
 * <p>This API supports:
 * <ul>
 *     <li>Customers uploading and resubmitting KYC documents</li>
 *     <li>Checking KYC verification status</li>
 *     <li>Admin verification or rejection of KYC</li>
 *     <li>Viewing pending KYC records (Admin)</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/kyc</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "6. KYC Management", description = "APIs for managing KYC verification process")
@RequestMapping("/api/v1/kyc")
public interface KycApi {

    /**
     * Upload or resubmit a KYC document.
     *
     * <p>This endpoint allows customers to:
     * <ul>
     *     <li>Submit KYC for the first time</li>
     *     <li>Resubmit documents after rejection</li>
     * </ul>
     *
     * <p>Supported document types may include:
     * <ul>
     *     <li>AADHAAR</li>
     *     <li>PAN</li>
     *     <li>PASSPORT</li>
     * </ul>
     *
     * @param principal authenticated user
     * @param documentType type of document (e.g., AADHAAR, PAN)
     * @param documentNumber unique document number
     * @param file uploaded document file
     * @return success message
     */
    @Operation(
            summary = "Upload or resubmit KYC document",
            description = "Allows customers to upload or resubmit KYC documents for verification"
    )
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

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Document type (AADHAAR, PAN, PASSPORT)",
                    example = "PAN",
                    required = true
            )
            @RequestParam String documentType,

            @Parameter(
                    description = "Document number",
                    example = "ABCDE1234F",
                    required = true
            )
            @RequestParam String documentNumber,

            @Parameter(
                    description = "KYC document file",
                    required = true
            )
            @RequestPart MultipartFile file
    );

    /**
     * Get KYC status for the logged-in customer.
     *
     * <p>Returns the current verification status such as:
     * <ul>
     *     <li>PENDING</li>
     *     <li>VERIFIED</li>
     *     <li>REJECTED</li>
     * </ul>
     *
     * @param principal authenticated user
     * @param status optional filter (rare use case)
     * @return KYC status response
     */
    @Operation(
            summary = "Get logged-in customer KYC status",
            description = "Fetch current KYC verification status of the authenticated user"
    )
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
    @GetMapping
    ResponseEntity<ApiResponse<KycResponse>> getStatus(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Optional filter by KYC status",
                    example = "PENDING"
            )
            @RequestParam(required = false) KycStatus status
    );

    /**
     * Verify or reject a KYC record (Admin only).
     *
     * <p>Admins can:
     * <ul>
     *     <li>Mark KYC as VERIFIED</li>
     *     <li>Reject with a reason</li>
     * </ul>
     *
     * @param kycId KYC record ID
     * @param principal authenticated admin
     * @param request verification request payload
     * @return success message
     */
    @Operation(
            summary = "Admin verify or reject KYC",
            description = "Allows admin to verify or reject KYC documents"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KYC processed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "KYC record not found"
            )
    })
    @PutMapping("/{kycId}")
    ResponseEntity<ApiResponse<String>> verify(

            @Parameter(
                    description = "KYC record ID",
                    required = true,
                    example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
            )
            @PathVariable UUID kycId,

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "KYC verification request",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = KycStatusUpdateRequest.class)
                    )
            )
            @Valid @RequestBody KycStatusUpdateRequest request
    );

    /**
     * Get all pending KYC records (Admin only).
     *
     * <p>This endpoint is used by admins to review pending KYC submissions.
     *
     * @return list of pending KYC records
     */
    @Operation(
            summary = "Get all pending KYC records (Admin)",
            description = "Fetch all KYC records that are pending verification"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pending KYC list fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = KycResponse.class)
                    )
            )
    })
    @GetMapping("/pending")
    ResponseEntity<ApiResponse<List<KycResponse>>> pending();
}