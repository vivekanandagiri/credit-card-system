package com.example.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing how a payment is allocated to a billing statement.
 *
 * <p>This object captures the allocation details during payment processing,
 * including how much was applied and the remaining balances before and after
 * allocation.</p>
 *
 * <h3>Key Details</h3>
 * <ul>
 *     <li>Tracks allocation at statement level</li>
 *     <li>Ensures financial consistency using {@link BigDecimal}</li>
 *     <li>Used in payment workflows and audit trails</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents allocation of a payment to a billing statement")
public class PaymentAllocationResponse {

    /**
     * Unique identifier of the billing statement.
     */
    @NotNull
    @Schema(
            description = "Unique identifier of the billing statement",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID statementId;

    /**
     * Amount allocated from the payment to this statement.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 15, fraction = 2)
    @Schema(
            description = "Amount allocated to this statement",
            example = "1500.50",
            minimum = "0.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal allocatedAmount;

    /**
     * Remaining due amount before allocation was applied.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 15, fraction = 2)
    @Schema(
            description = "Remaining balance before allocation",
            example = "5000.00",
            minimum = "0.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal remainingBeforeAllocation;

    /**
     * Remaining due amount after allocation was applied.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 15, fraction = 2)
    @Schema(
            description = "Remaining balance after allocation",
            example = "3500.00",
            minimum = "0.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal remainingAfterAllocation;
}