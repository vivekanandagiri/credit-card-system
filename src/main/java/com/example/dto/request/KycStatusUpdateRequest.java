package com.example.dto.request;

import com.example.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating the KYC status of a customer.
 * <p>
 * This request is typically used by admin or verification systems
 * to approve or reject submitted KYC documents.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for updating KYC status")
public class KycStatusUpdateRequest {

    /**
     * New KYC status.
     * Example values: PENDING, VERIFIED, REJECTED
     */
    @NotNull(message = "KYC status is required")
    @Schema(
        description = "New status of the KYC",
        example = "VERIFIED",
        implementation = KycStatus.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private KycStatus status;

    /**
     * Reason for rejection.
     * Required only when status is REJECTED.
     */
    @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
    @Schema(
        description = "Reason for rejection (required if status is REJECTED)",
        example = "Document image is unclear"
    )
    private String rejectionReason;
}