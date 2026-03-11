package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for verifying or rejecting KYC")
public class KycVerifyRequest {

    @Schema(
            description = "Indicates whether the KYC is approved or rejected",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean approved;

    @Schema(
            description = "Reason for rejection (required if approved is false)",
            example = "Document image is unclear"
    )
    @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
    private String rejectionReason;
}