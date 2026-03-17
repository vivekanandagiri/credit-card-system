package com.example.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response containing details of a customer's credit account")
public class CreditAccountResponse {

@Schema(
        description = "Unique identifier of the credit account",
        example = "d7f3a2a1-93a4-4e2c-8b92-b8a0a7e6e9e3",
        accessMode = Schema.AccessMode.READ_ONLY
)
private UUID accountId;

@Schema(
        description = "Unique credit card account number",
        example = "4112345678901234",
        accessMode = Schema.AccessMode.READ_ONLY
)
private String accountNumber;

// Customer info

@Schema(
        description = "Unique identifier of the customer",
        example = "0818473a-74fb-49d5-a911-248aaa3d0ade"
)
private UUID customerId;

@Schema(
        description = "Full name of the customer",
        example = "Vivek Kumar"
)
private String customerName;

// Product info

@Schema(
        description = "Credit Product Id",
        example = "123"
)
private Long creditProductId;

@Schema(
        description = "Name of the credit product",
        example = "SBI Card PRIME"
)
private String creditProductName;

// Application reference

@Schema(
        description = "Reference to the credit card application that created this account",
        example = "f6d14ff2-9d31-48a7-a761-79eabf8e0aa1"
)
private UUID applicationId;

// Status

@Schema(
        description = "Current account status",
        example = "ACTIVE"
)
private String accountStatus;

// Credit terms

@Schema(
        description = "Approved credit limit for the account",
        example = "200000"
)
private BigDecimal creditLimit;

@Schema(
        description = "Annual Percentage Rate (APR) applied to the account",
        example = "14.5"
)
private BigDecimal apr;

@Schema(
        description = "Current outstanding balance",
        example = "35000"
)
private BigDecimal currentBalance;

@Schema(
        description = "Available balance remaining for spending",
        example = "165000"
)
private BigDecimal availableBalance;

// Billing

@Schema(
        description = "Day of month when the billing statement is generated",
        example = "5"
)
private Integer statementCycleDay;

@Schema(
        description = "Date when the last statement was generated",
        example = "2026-03-05"
)
private LocalDate lastStatementDate;

@Schema(
        description = "Balance amount from the last statement",
        example = "30000"
)
private BigDecimal lastStatementBalance;

@Schema(
        description = "Next payment due date",
        example = "2026-03-25"
)
private LocalDate nextDueDate;

@Schema(
        description = "Minimum payment amount due",
        example = "1500"
)
private BigDecimal minimumDueAmount;

@Schema(
        description = "Date of the last payment made",
        example = "2026-03-10"
)
private LocalDate lastPaymentDate;

@Schema(
        description = "Amount of the last payment made",
        example = "5000"
)
private BigDecimal lastPaymentAmount;

// Life cycle

@Schema(
        description = "Timestamp when the account was activated",
        example = "2026-03-01T10:15:30Z"
)
private Instant activatedAt;

@Schema(
        description = "Timestamp when the account was closed (if applicable)",
        example = "2028-05-10T14:30:00Z"
)
private Instant closedAt;


}
