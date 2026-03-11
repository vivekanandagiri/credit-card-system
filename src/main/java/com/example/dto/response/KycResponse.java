package com.example.dto.response;

import com.example.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(description = "Response containing KYC submission details")
public class KycResponse {

    @Schema(
            description = "Unique identifier of the KYC record",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID kycId;

    @Schema(
            description = "Current KYC verification status",
            example = "PENDING"
    )
    private KycStatus status;

    @Schema(
            description = "Timestamp when KYC was submitted (UTC)",
            example = "2026-03-03T12:30:45Z"
    )
    private Instant submittedAt;
}