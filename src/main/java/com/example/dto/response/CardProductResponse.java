package com.example.dto.response;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(description = "Card product configuration details")
public class CardProductResponse {

    @Schema(description = "Unique identifier of the card product",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID cardProductId;

    @Schema(description = "Associated credit product ID",
            example = "1")
    private Long creditProductId;

    @Schema(description = "Associated credit product name",
            example = "Gold Credit Product")
    private String creditProductName;

    @Schema(description = "Card product display name",
            example = "Gold Visa Card")
    private String productName;

    @Schema(description = "Card network provider",
            example = "VISA")
    private NetworkType networkType;

    @Schema(description = "Type of card",
            example = "CREDIT")
    private CardType cardType;

    @Schema(description = "Annual fee charged",
            example = "1999.00")
    private BigDecimal annualFee;

    @Schema(description = "Card validity in years",
            example = "5")
    private Integer cardValidityYears;

    @Schema(description = "Whether contactless payments are enabled",
            example = "true")
    private Boolean contactlessEnabled;

    @Schema(description = "Whether international usage is allowed",
            example = "true")
    private Boolean internationalUsageAllowed;

    @Schema(description = "Whether online transactions are allowed",
            example = "true")
    private Boolean onlineTransactionsAllowed;

    @Schema(description = "Whether ATM withdrawals are allowed",
            example = "true")
    private Boolean atmWithdrawalAllowed;

    @Schema(description = "ATM daily withdrawal limit",
            example = "30000.00")
    private BigDecimal atmDailyLimit;

    @Schema(description = "POS daily spending limit",
            example = "150000.00")
    private BigDecimal posDailyLimit;

    @Schema(description = "E-commerce daily spending limit",
            example = "100000.00")
    private BigDecimal ecommerceDailyLimit;

    @Schema(description = "Statement cycle day (1–28)",
            example = "20")
    private Integer statementCycleDay;

    @Schema(description = "Forex markup percentage",
            example = "2.99")
    private BigDecimal forexMarkupPercent;

    @Schema(description = "Card product description",
            example = "Premium card with airport lounge access")
    private String productDescription;

    @Schema(description = "Current card product status",
            example = "ACTIVE")
    private ProductStatus status;
}