package com.example.dto.response;

import com.example.enums.StatementStatus;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Response DTO representing a credit card billing statement.
 *
 * <p>This object encapsulates all financial and lifecycle details of a billing cycle,
 * including balances, transactions, dues, and payment status.</p>
 *
 * <h3>Key Features</h3>
 * <ul>
 *     <li>Captures full billing cycle financial snapshot</li>
 *     <li>Includes payment tracking and due information</li>
 *     <li>Supports audit and reconciliation use cases</li>
 * </ul>
 */
@Data
@Schema(description = "Represents a credit card billing statement")
public class BillingStatementResponse {

    @NotNull
    @Schema(
            description = "Unique statement identifier",
            example = "f3fa85f6-1111-4567-b3fc-2c963f66afa6"
    )
    private UUID statementId;

    @NotNull
    @Schema(
            description = "Associated credit account ID",
            example = "a2fa85f6-2222-4567-b3fc-2c963f66afa6"
    )
    private UUID accountId;

    @NotBlank
    @Schema(
            description = "Masked or full account number",
            example = "XXXX-XXXX-XXXX-1234"
    )
    private String accountNumber;

    @NotNull
    @Schema(
            description = "Start date of billing period",
            example = "2026-03-01"
    )
    private LocalDate billingPeriodStart;

    @NotNull
    @Schema(
            description = "End date of billing period",
            example = "2026-03-31"
    )
    private LocalDate billingPeriodEnd;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "1000.00", description = "Opening balance at start of cycle")
    private BigDecimal openingBalance;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "2500.00", description = "Total debits (spends)")
    private BigDecimal totalDebits;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "500.00", description = "Total credits (payments)")
    private BigDecimal totalCredits;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "150.00", description = "Interest charged")
    private BigDecimal interestCharged;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "3150.00", description = "Remaining unpaid balance")
    private BigDecimal remainingAmount;

    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "100.00", description = "Late fee applied (if any)")
    private BigDecimal lateFee;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "3150.00", description = "Closing balance")
    private BigDecimal closingBalance;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "3150.00", description = "Total amount due")
    private BigDecimal totalAmountDue;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "500.00", description = "Minimum payment required")
    private BigDecimal minimumDueAmount;

    @NotNull
    @Schema(example = "2026-04-10", description = "Payment due date")
    private LocalDate dueDate;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Schema(example = "1000.00", description = "Amount paid towards this statement")
    private BigDecimal amountPaid;

    @NotNull
    @Schema(
            description = "Current status of the statement",
            example = "GENERATED",
            allowableValues = {"GENERATED", "PAID", "REVOLVING", "OVERDUE"}
    )
    private StatementStatus statementStatus;

    @NotNull
    @Schema(
            description = "Timestamp when statement was generated (UTC)",
            example = "2026-04-01T10:15:30Z"
    )
    private Instant generatedAt;
}