package com.example.dto.request;

import java.math.BigDecimal;

import com.example.enums.Currency;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for processing a financial transaction.
 * <p>
 * This request includes transaction details such as type, channel,
 * amount, merchant details, and reference identifiers.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for processing a transaction")
public class TransactionRequest {

    /**
     * Type of transaction (e.g., PURCHASE, REFUND).
     */
    @NotNull(message = "Transaction type is required")
    @Schema(
        description = "Type of transaction",
        example = "PURCHASE",
        allowableValues = {"PURCHASE"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TransactionType transactionType;

    /**
     * Channel through which the transaction is performed (e.g., ONLINE, POS, ATM).
     */
    @NotNull(message = "Transaction channel is required")
    @Schema(
        description = "Channel of transaction",
        example = "ONLINE",
        allowableValues = {"ONLINE", "POS", "ATM"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TransactionChannel transactionChannel;

    /**
     * Transaction amount.
     * Must be greater than zero and up to 2 decimal places.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must be a valid monetary value")
    @Schema(
        description = "Transaction amount",
        example = "1500.50",
        minimum = "0.01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    /**
     * Currency of the transaction.
     * Defaults to INR.
     */
    @NotNull(message = "Currency is required")
    @Schema(
        description = "Transaction currency",
        example = "INR",
        implementation = Currency.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Currency currency = Currency.INR;

    /**
     * Name of the merchant.
     */
    @Size(max = 100, message = "Merchant name cannot exceed 100 characters")
    @Schema(
        description = "Merchant name",
        example = "Amazon India"
    )
    private String merchantName;

    /**
     * Merchant Category Code (MCC).
     * Must be exactly 4 digits.
     */
    @Pattern(regexp = "^\\d{4}$", message = "MCC must be exactly 4 digits (e.g., 5411)")
    @Schema(
        description = "Merchant Category Code (MCC)",
        example = "5411"
    )
    private String merchantCategoryCode;

    /**
     * Merchant category name.
     */
    @Size(max = 100, message = "Merchant category name cannot exceed 100 characters")
    @Schema(
        description = "Merchant category name",
        example = "Grocery Stores"
    )
    private String merchantCategoryName;

    /**
     * Unique transaction reference identifier.
     */
    @NotBlank(message = "Transaction reference is required")
    @Size(max = 50, message = "Transaction reference cannot exceed 50 characters")
    @Schema(
        description = "Unique transaction reference identifier",
        example = "TXN123456789",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String transactionReference;
}