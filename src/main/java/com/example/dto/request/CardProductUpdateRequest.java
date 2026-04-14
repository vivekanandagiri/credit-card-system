package com.example.dto.request;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(
    description = "Request object for updating an existing card product (partial update supported). " +
                  "Only provided fields will be updated."
)
public class CardProductUpdateRequest {

    @Schema(description = "Updated card product display name", example = "Platinum Visa Card")
    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    @Schema(description = "Card network provider", example = "VISA", implementation = NetworkType.class)
    private NetworkType networkType;

    @Schema(description = "Card type", example = "CREDIT", implementation = CardType.class)
    private CardType cardType;

    @Schema(description = "Annual fee", example = "2999.00", minimum = "0")
    @DecimalMin(value = "0.0", message = "Annual fee cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal annualFee;

    @Schema(description = "Card validity in years (1–10)", example = "5", minimum = "1", maximum = "10")
    @Min(value = 1, message = "Minimum validity is 1 year")
    @Max(value = 10, message = "Maximum validity is 10 years")
    private Integer cardValidityYears;

    @Schema(description = "Enable/disable contactless payments", example = "true")
    private Boolean contactlessEnabled;

    @Schema(description = "Enable/disable international transactions", example = "true")
    private Boolean internationalUsageAllowed;

    @Schema(description = "Enable/disable online transactions", example = "true")
    private Boolean onlineTransactionsAllowed;

    @Schema(description = "Enable/disable ATM withdrawals", example = "true")
    private Boolean atmWithdrawalAllowed;

    @Schema(description = "ATM daily withdrawal limit", example = "30000.00", minimum = "0")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal atmDailyLimit;

    @Schema(description = "POS daily spending limit", example = "150000.00", minimum = "0")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal posDailyLimit;

    @Schema(description = "E-commerce daily spending limit", example = "100000.00", minimum = "0")
    @DecimalMin(value = "0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal ecommerceDailyLimit;

    @Schema(description = "Statement cycle day (1–28)", example = "20", minimum = "1", maximum = "28")
    @Min(value = 1, message = "Statement cycle must be >= 1")
    @Max(value = 28, message = "Statement cycle must be <= 28")
    private Integer statementCycleDay;

    @Schema(description = "Forex markup percentage", example = "2.99", minimum = "0", maximum = "100")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal forexMarkupPercent;

    @Schema(description = "Product description", example = "Premium platinum card with airport lounge access", maxLength = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String productDescription;

    @Schema(description = "Product status", example = "ACTIVE", implementation = ProductStatus.class)
    private ProductStatus status;

    // ------------------ CROSS-FIELD VALIDATIONS ------------------

    @AssertTrue(message = "ATM limit must be provided when ATM withdrawals are enabled")
    public boolean isAtmLimitValid() {
        if (Boolean.TRUE.equals(atmWithdrawalAllowed)) {
            return atmDailyLimit != null && atmDailyLimit.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    @AssertTrue(message = "E-commerce limit must be provided when online transactions are enabled")
    public boolean isEcommerceLimitValid() {
        if (Boolean.TRUE.equals(onlineTransactionsAllowed)) {
            return ecommerceDailyLimit != null && ecommerceDailyLimit.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    @AssertTrue(message = "POS limit must be provided when card is active")
    public boolean isPosLimitValid() {
        if (status == ProductStatus.ACTIVE) {
            return posDailyLimit != null && posDailyLimit.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    @AssertTrue(message = "International usage must have forex markup defined")
    public boolean isForexMarkupValid() {
        if (Boolean.TRUE.equals(internationalUsageAllowed)) {
            return forexMarkupPercent != null;
        }
        return true;
    }
}