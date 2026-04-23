package com.example.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;

/**
 * Response DTO representing a processed payment.
 *
 * <p>This object encapsulates all details related to a payment transaction,
 * including metadata, status, and allocation breakdown across billing statements.</p>
 *
 * <h3>Key Features</h3>
 * <ul>
 *     <li>Tracks payment lifecycle (status, timestamp)</li>
 *     <li>Includes allocation details for transparency</li>
 *     <li>Ensures financial precision using {@link BigDecimal}</li>
 * </ul>
 */
@Builder
@Data
@Schema(description = "Represents a payment transaction and its allocation details")
public class PaymentResponse {

    /**
     * Unique identifier of the payment.
     */
    @NotNull
    @Schema(
            description = "Unique payment identifier",
            example = "a3fa85f6-1234-4567-b3fc-2c963f66afa6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID paymentId;

    /**
     * Identifier of the credit account associated with the payment.
     */
    @NotNull
    @Schema(
            description = "Associated credit account ID",
            example = "b2fa85f6-5678-4567-b3fc-2c963f66afa6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID accountId;

    /**
     * Total payment amount.
     */
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 15, fraction = 2)
    @Schema(
            description = "Total payment amount",
            example = "2500.00",
            minimum = "0.01",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    /**
     * Current status of the payment.
     */
    @NotNull
    @Schema(
            description = "Payment processing status",
            example = "SUCCESS",
            allowableValues = {"PENDING", "SUCCESS", "FAILED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentStatus status;

    /**
     * Method used to perform the payment.
     */
    @NotNull
    @Schema(
            description = "Payment method used",
            example = "UPI",
            allowableValues = {"UPI", "CARD", "NET_BANKING", "WALLET"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentMethod paymentMethod;

    /**
     * External or client-provided reference ID.
     *
     * <p>Used for idempotency and tracking.</p>
     */
    @NotBlank
    @Size(max = 100)
    @Schema(
            description = "External reference ID for idempotency",
            example = "txn_20260422_ABC123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String referenceId;

    /**
     * Timestamp when the payment was completed.
     */
    @NotNull
    @Schema(
            description = "Timestamp when payment was completed",
            example = "2026-04-22T15:45:30+05:30",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant paidAt;

    /**
     * Allocation breakdown of this payment across billing statements.
     */
    @NotNull
    @Valid
    @Size(min = 0)
    @Schema(
            description = "List of allocations applied to billing statements"
    )
    private List<PaymentAllocationResponse> allocations;
}