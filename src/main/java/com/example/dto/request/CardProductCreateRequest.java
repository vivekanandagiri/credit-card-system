package com.example.dto.request;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request object for creating a card product variant under a credit product")
public class CardProductCreateRequest {


    @Schema(description = "Card product display name",
            example = "Gold Visa Card",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product name is required")
    @Size(max = 100)
    private String productName;

    @Schema(description = "Card network provider",
            example = "VISA",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Network type is required")
    private NetworkType networkType;

    @Schema(description = "Type of card",
            example = "CREDIT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Card type is required")
    private CardType cardType;

    @Schema(description = "Annual fee charged for the card",
            example = "1999.00")
    @DecimalMin(value = "0.0", message = "Annual fee cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal annualFee;

    @Schema(description = "Card validity period in years",
            example = "5")
    @Min(value = 1)
    @Max(value = 10)
    private Integer cardValidityYears;

    @Schema(description = "Whether contactless payments are enabled",
            example = "true")
    private Boolean contactlessEnabled;

    @Schema(description = "Whether international transactions are allowed",
            example = "true")
    private Boolean internationalUsageAllowed;

    @Schema(description = "Whether online transactions are allowed",
            example = "true")
    private Boolean onlineTransactionsAllowed;

    @Schema(description = "Whether ATM withdrawals are allowed",
            example = "true")
    private Boolean atmWithdrawalAllowed;

    @Schema(description = "ATM daily withdrawal limit",
            example = "25000.00")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal atmDailyLimit;

    @Schema(description = "POS daily spending limit",
            example = "100000.00")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal posDailyLimit;

    @Schema(description = "E-commerce daily spending limit",
            example = "75000.00")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal ecommerceDailyLimit;

    @Schema(description = "Statement cycle day (1–28)",
            example = "15")
    @Min(value = 1)
    @Max(value = 28)
    private Integer statementCycleDay;

    @Schema(description = "Forex markup percentage",
            example = "3.50")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal forexMarkupPercent;

    @Schema(description = "Detailed description of the card product",
            example = "Premium gold card with reward points and lounge access")
    @Size(max = 500)
    private String productDescription;
}