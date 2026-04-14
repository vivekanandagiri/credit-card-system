package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.ApplicationStatus;
import com.example.enums.DecisionType;
import com.example.enums.EmploymentType;


import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Response containing details of a credit card application")
public class CreditCardApplicationResponse {


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

@Schema(
    description = "Full name of the customer",
    example = "Amit Kumar"
)
private String customerName;

// Card product info

@Schema(
    description = "Unique id of the credit product",
    example = "123"
)
private Long creditProductId;

@Schema(
    description = "Name of the credit  product",
    example = "SBI Rewards Credit Card"
)
private String creditProductName;

@Schema(
    description = "Credit Product code",
    example = "GOLD-CREDIT-PRODUCT-001"
)
private String productCode;

// Application data

@Schema(
    description = "Employment type of the applicant",
    example = "SALARIED"
)
private EmploymentType employmentType;

@Schema(
    description = "Employer name (if applicant is salaried)",
    example = "TCS"
)
private String employerName;

@Schema(
    description = "Monthly income declared by the applicant",
    example = "75000"
)
private BigDecimal monthlyIncome;

@Schema(
    description = "Total existing liabilities (EMIs, loans etc.)",
    example = "15000"
)
private BigDecimal existingLiabilities;

@Schema(
    description = "Credit score recorded at the time of application",
    example = "720",
    minimum = "300",
    maximum = "900"
)
private Integer creditScoreAtApplication;

@Schema(
    description = "Credit limit requested by the applicant",
    example = "200000"
)
private BigDecimal requestedCreditLimit;

// Decision

@Schema(
    description = "Current status of the application",
    example = "PENDING_REVIEW"
)
private ApplicationStatus applicationStatus;

@Schema(
    description = "Decision taken by the underwriting system or admin",
    example = "AUTO_APPROVED"
)
private DecisionType decision;

@Schema(
    description = "Reason for approval or rejection",
    example = "Customer meets credit score and income criteria"
)
private String decisionReason;

@Schema(
    description = "Approved credit limit (if application is approved)",
    example = "180000"
)
private BigDecimal approvedCreditLimit;

@Schema(
    description = "Approved annual percentage rate (APR)",
    example = "14.5"
)
private BigDecimal approvedApr;

// Timestampz

@Schema(
    description = "Timestamp when the application was submitted",
    example = "2026-03-12T12:29:47Z"
)
private Instant submittedAt;

@Schema(
    description = "Timestamp when the decision was made",
    example = "2026-03-12T12:30:15Z"
)
private Instant decisionAt;

}
