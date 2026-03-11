package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request object for updating an existing credit product (partial update supported)")
public class CreditProductUpdateRequest {

	@Schema(description = "Updated product display name", example = "Platinum Credit Card")
	@Size(max = 100, message = "Product name cannot exceed 100 characters")
	private String productName;

	@Schema(description = "Minimum credit limit", example = "75000.00")
	@DecimalMin(value = "0.0", inclusive = false, message = "Minimum credit limit must be positive")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal minCreditLimit;

	@Schema(description = "Maximum credit limit", example = "750000.00")
	@DecimalMin(value = "0.0", inclusive = false, message = "Maximum credit limit must be positive")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal maxCreditLimit;

	@Schema(description = "Minimum annual income required", example = "500000.00")
	@DecimalMin(value = "0.0", inclusive = false)
	@Digits(integer = 12, fraction = 2)
	private BigDecimal minIncomeRequired;

	@Schema(description = "Minimum required credit score (300–900)", example = "720")
	@Min(value = 300)
	@Max(value = 900)
	private Integer minCreditScore;

	@Schema(description = "Purchase APR (%)", example = "12.99")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal aprPurchase;

	@Schema(description = "Cash advance APR (%)", example = "24.99")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
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
	@Digits(integer = 3, fraction = 2)
	private BigDecimal minimumDuePercent;

	@Schema(description = "Minimum due fixed amount", example = "1000.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal minimumDueAmount;

	@Schema(description = "Late fee amount", example = "750.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal lateFeeAmount;

	@Schema(description = "Overlimit fee", example = "500.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal overlimitFee;

	@Schema(description = "Joining fee", example = "1999.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal joiningFee;

	@Schema(description = "Foreign transaction fee (%)", example = "3.50")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal foreignTransactionFeePercent;

	@Schema(description = "Balance transfer fee (%)", example = "2.50")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal balanceTransferFeePercent;

	@Schema(description = "Cash advance fee (%)", example = "2.00")
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "100.0")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal cashAdvanceFeePercent;

	@Schema(description = "Minimum cash advance fee amount", example = "300.00")
	@DecimalMin(value = "0.0")
	@Digits(integer = 12, fraction = 2)
	private BigDecimal cashAdvanceFeeMin;

	@Schema(description = "Updated effective start date", example = "2026-01-01")
	@FutureOrPresent(message = "Effective from date must be today or future")
	private LocalDate effectiveFrom;

	@Schema(description = "Updated effective end date", example = "2032-12-31")
	@Future(message = "Effective to date must be in the future")
	private LocalDate effectiveTo;
}