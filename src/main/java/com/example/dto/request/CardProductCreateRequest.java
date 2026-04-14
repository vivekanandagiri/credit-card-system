package com.example.dto.request;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating a card product variant.
 * <p>
 * Defines card configuration such as network, fees, limits,
 * usage permissions, and billing cycle details.
 * </p>
 */
@Data
@Schema(description = "Request object for creating a card product variant under a credit product")
public class CardProductCreateRequest {

    /**
     * Display name of the card product.
     */
    @Schema(
        description = "Card product display name",
        example = "Gold Visa Card",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    /**
     * Card network provider (e.g., VISA, MASTERCARD).
     */
    @Schema(
        description = "Card network provider",
        example = "VISA",
        implementation = NetworkType.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Network type is required")
    private NetworkType networkType;

    /**
     * Type of card (e.g., CREDIT, DEBIT).
     */
    @Schema(
        description = "Type of card",
        example = "CREDIT",
        implementation = CardType.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Card type is required")
    private CardType cardType;

    /**
     * Annual fee charged for the card.
     */
    @Schema(
        description = "Annual fee charged for the card",
        example = "1999.00"
    )
    @DecimalMin(value = "0.0", message = "Annual fee cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid annual fee format")
    private BigDecimal annualFee;

    /**
     * Validity period of the card in years.
     */
    @Schema(
        description = "Card validity period in years",
        example = "5"
    )
    @Min(value = 1, message = "Card validity must be at least 1 year")
    @Max(value = 10, message = "Card validity cannot exceed 10 years")
    private Integer cardValidityYears;

    /**
     * Indicates whether contactless payments are enabled.
     */
    @Schema(description = "Whether contactless payments are enabled", example = "true")
    private Boolean contactlessEnabled;

    /**
     * Indicates whether international transactions are allowed.
     */
    @Schema(description = "Whether international transactions are allowed", example = "true")
    private Boolean internationalUsageAllowed;

    /**
     * Indicates whether online transactions are allowed.
     */
    @Schema(description = "Whether online transactions are allowed", example = "true")
    private Boolean onlineTransactionsAllowed;

    /**
     * Indicates whether ATM withdrawals are allowed.
     */
    @Schema(description = "Whether ATM withdrawals are allowed", example = "true")
    private Boolean atmWithdrawalAllowed;

    /**
     * Daily ATM withdrawal limit.
     */
    @Schema(description = "ATM daily withdrawal limit", example = "25000.00")
    @DecimalMin(value = "0.0", message = "ATM daily limit cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Invalid ATM limit format")
    private BigDecimal atmDailyLimit;

    /**
     * Daily POS (Point of Sale) spending limit.
     */
    @Schema(description = "POS daily spending limit", example = "100000.00")
    @DecimalMin(value = "0.0", message = "POS daily limit cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Invalid POS limit format")
    private BigDecimal posDailyLimit;

    /**
     * Daily e-commerce spending limit.
     */
    @Schema(description = "E-commerce daily spending limit", example = "75000.00")
    @DecimalMin(value = "0.0", message = "E-commerce limit cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Invalid e-commerce limit format")
    private BigDecimal ecommerceDailyLimit;

    /**
     * Billing statement cycle day (1–28).
     */
    @Schema(description = "Statement cycle day (1–28)", example = "15")
    @Min(value = 1, message = "Statement cycle day must be at least 1")
    @Max(value = 28, message = "Statement cycle day cannot exceed 28")
    private Integer statementCycleDay;

    /**
     * Forex markup percentage applied on international transactions.
     */
    @Schema(description = "Forex markup percentage", example = "3.50")
    @DecimalMin(value = "0.0", message = "Forex markup cannot be negative")
    @DecimalMax(value = "100.0", message = "Forex markup cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Invalid forex markup format")
    private BigDecimal forexMarkupPercent;

    /**
     * Detailed description of the card product.
     */
    @Schema(
        description = "Detailed description of the card product",
        example = "Premium gold card with reward points and lounge access"
    )
    @Size(max = 500, message = "Product description cannot exceed 500 characters")
    private String productDescription;
}