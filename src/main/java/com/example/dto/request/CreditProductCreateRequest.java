package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request object for creating a credit product")
public class CreditProductCreateRequest {


	@Schema(description = "Product display name", example = "Gold Credit Card", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Product name is required")
	@Size(max = 100)
	private String productName;

	@Schema(description = "Minimum credit limit allowed", example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@DecimalMin(value = "0.0", inclusive = false, message = "Minimum credit limit must be positive")
	private BigDecimal minCreditLimit;

	@Schema(description = "Maximum credit limit allowed", example = "500000.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@DecimalMin(value = "0.0", inclusive = false, message = "Maximum credit limit must be positive")
	private BigDecimal maxCreditLimit;

	@Schema(description = "Minimum annual income required", example = "300000.00")
	@DecimalMin(value = "0.0", inclusive = false)
	private BigDecimal minIncomeRequired;

	@Schema(description = "Minimum required credit score (300–900)", example = "700")
	@Min(value = 300)
	@Max(value = 900)
	private Integer minCreditScore;

	@Schema(description = "Purchase APR (%)", example = "14.99")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal aprPurchase;

	@Schema(description = "Cash advance APR (%)", example = "24.99")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal aprCashAdvance;

	@Schema(description = "Grace period in days", example = "45")
	@Min(value = 0)
	@Max(value = 90)
	private Integer gracePeriodDays;

	@Schema(description = "Interest calculation method", example = "DAILY_REDUCING_BALANCE")
	@Size(max = 50)
	private String interestCalculationMethod;

	@Schema(description = "Minimum due percentage (%)", example = "5.00")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal minimumDuePercent;

	@Schema(description = "Minimum due fixed amount", example = "1000.00")
	@DecimalMin(value = "0.0")
	private BigDecimal minimumDueAmount;

	@Schema(description = "Late fee amount", example = "750.00")
	@DecimalMin(value = "0.0")
	private BigDecimal lateFeeAmount;

	@Schema(description = "Overlimit fee", example = "500.00")
	@DecimalMin(value = "0.0")
	private BigDecimal overlimitFee;

	@Schema(description = "Joining fee", example = "1999.00")
	@DecimalMin(value = "0.0")
	private BigDecimal joiningFee;

	@Schema(description = "Foreign transaction fee (%)", example = "3.50")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal foreignTransactionFeePercent;

	@Schema(description = "Balance transfer fee (%)", example = "2.50")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal balanceTransferFeePercent;

	@Schema(description = "Cash advance fee (%)", example = "2.00")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	private BigDecimal cashAdvanceFeePercent;

	@Schema(description = "Minimum cash advance fee amount", example = "300.00")
	@DecimalMin(value = "0.0")
	private BigDecimal cashAdvanceFeeMin;

	@Schema(description = "Effective start date of the product", example = "2026-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@FutureOrPresent(message = "Effective from date must be today or future")
	private LocalDate effectiveFrom;

	@Schema(description = "Effective end date of the product", example = "2030-12-31")
	@Future(message = "Effective to date must be in the future")
	private LocalDate effectiveTo;
}