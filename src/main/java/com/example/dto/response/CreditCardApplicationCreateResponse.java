package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

import com.example.enums.ApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@Schema(description = "Response containing details of a credit card application")
public class CreditCardApplicationCreateResponse {

@Schema(
    description = "Unique identifier of the application",
    example = "f6d14ff2-9d31-48a7-a761-79eabf8e0aa1"
)
private UUID applicationId;

// Customer info

@Schema(
    description = "Unique identifier of the customer",
    example = "0818473a-74fb-49d5-a911-248aaa3d0ade"
)
private UUID customerId;

// Card product info
@Schema(
    description = "Unique id of the credit product",
    example = "123"
)
private Long creditProductId;
// Decision
@Schema(
    description = "Current status of the application",
    example = "PENDING_REVIEW"
)
private ApplicationStatus applicationStatus;

@Schema(
	    description = "Application Decission Reason",
	    example = "Rejected by rule: Minimum Annual Income"
	)
private String decisionReason;

// Timestampz
@Schema(
    description = "Timestamp when the application was submitted",
    example = "2026-03-12T12:29:47Z"
)
private Instant submittedAt;

}
