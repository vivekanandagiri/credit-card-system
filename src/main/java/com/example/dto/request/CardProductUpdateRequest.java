package com.example.dto.request;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request object for updating an existing card product (partial update supported)")
public class CardProductUpdateRequest {

	@Schema(description = "Updated card product display name", example = "Platinum Visa Card")
	@Size(max = 100, message = "Product name cannot exceed 100 characters")
	private String productName;

	@Schema(description = "Updated card network provider", example = "VISA")
	private NetworkType networkType;

	@Schema(description = "Updated card type", example = "CREDIT")
	private CardType cardType;

	@Schema(description = "Updated annual fee", example = "2999.00")
	@DecimalMin(value = "0.0", message = "Annual fee cannot be negative")
	@Digits(integer = 10, fraction = 2)
	private BigDecimal annualFee;

	@Schema(description = "Updated card validity in years", example = "5")
	@Min(value = 1)
	@Max(value = 10)
	private Integer cardValidityYears;

	@Schema(description = "Enable/disable contactless payments", example = "true")
	private Boolean contactlessEnabled;

	@Schema(description = "Enable/disable international transactions", example = "true")
	private Boolean internationalUsageAllowed;

	@Schema(description = "Enable/disable online transactions", example = "true")
	private Boolean onlineTransactionsAllowed;

	@Schema(description = "Enable/disable ATM withdrawals", example = "true")
	private Boolean atmWithdrawalAllowed;

	@Schema(description = "Updated ATM daily withdrawal limit", example = "30000.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal atmDailyLimit;

	@Schema(description = "Updated POS daily spending limit", example = "150000.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal posDailyLimit;

	@Schema(description = "Updated e-commerce daily spending limit", example = "100000.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal ecommerceDailyLimit;

	@Schema(description = "Updated statement cycle day (1–28)", example = "20")
	@Min(value = 1)
	@Max(value = 28)
	private Integer statementCycleDay;

	@Schema(description = "Updated forex markup percentage", example = "2.99")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal forexMarkupPercent;

	@Schema(description = "Updated product description", example = "Premium platinum card with airport lounge access")
	@Size(max = 500)
	private String productDescription;
}