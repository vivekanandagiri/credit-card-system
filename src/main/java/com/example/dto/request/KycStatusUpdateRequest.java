package com.example.dto.request;

import com.example.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for updating KYC status")
public class KycStatusUpdateRequest {

    @NotNull
    @Schema(
            description = "New status of the KYC",
            example = "VERIFIED",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private KycStatus status;

    @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
    @Schema(
            description = "Reason for rejection (required if status is REJECTED)",
            example = "Document image is unclear"
    )
    private String rejectionReason;
}