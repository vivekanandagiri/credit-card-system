package com.example.dto.request;

import java.math.BigDecimal;

import com.example.enums.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for processing a payment against a billing statement.
 * <p>
 * Supports partial payments, minimum due payments, or full settlement.
 * </p>
 */
@Data
@Schema(description = "Request object for processing a payment against a statement")
public class PaymentRequest {

    /**
     * Amount to be paid by the customer.
     * Must be greater than zero and up to 2 decimal places.
     */
    @NotNull(message = "Payment amount must not be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format (max 10 digits and 2 decimal places)")
    @Schema(
        description = "Amount to be paid. Can be partial, minimum due, or full amount",
        example = "1500.75",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    /**
     * Payment method used by the customer (e.g., UPI, CARD, NET_BANKING).
     */
    @NotNull(message = "Payment method must not be null")
    @Schema(
        description = "Payment method selected by the customer",
        example = "UPI",
        implementation = PaymentMethod.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentMethod paymentMethod;

    /**
     * External reference ID for the payment.
     * Useful for tracking transactions across systems (e.g., UPI reference, bank txn ID).
     */
    @NotBlank(message = "Reference ID is required")
    @Size(max = 50, message = "Reference ID cannot exceed 50 characters")
    @Schema(
        description = "External payment reference identifier",
        example = "UPI123456789",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String referenceId;
}