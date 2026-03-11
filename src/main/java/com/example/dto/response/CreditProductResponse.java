package com.example.dto.response;

import com.example.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Credit product configuration details")
public class CreditProductResponse {

    @Schema(
            description = "Unique identifier of the credit product",
            example = "1"
    )
    private Long creditProductId;

    @Schema(
            description = "System-generated unique product code",
            example = "GOLD-CREDIT-CARD-001"
    )
    private String productCode;

    @Schema(
            description = "Product display name",
            example = "Gold Credit Card"
    )
    private String productName;

    @Schema(description = "Minimum credit limit", example = "50000.00")
    private BigDecimal minCreditLimit;

    @Schema(description = "Maximum credit limit", example = "500000.00")
    private BigDecimal maxCreditLimit;

    @Schema(description = "Minimum annual income required",
            example = "300000.00")
    private BigDecimal minIncomeRequired;

    @Schema(description = "Minimum required credit score (300–900)",
            example = "700")
    private Integer minCreditScore;

    @Schema(description = "Purchase APR (%)", example = "14.99")
    private BigDecimal aprPurchase;

    @Schema(description = "Cash advance APR (%)", example = "24.99")
    private BigDecimal aprCashAdvance;

    @Schema(description = "Grace period in days", example = "45")
    private Integer gracePeriodDays;

    @Schema(description = "Interest calculation method",
            example = "DAILY_REDUCING_BALANCE")
    private String interestCalculationMethod;

    @Schema(description = "Minimum due percentage (%)", example = "5.00")
    private BigDecimal minimumDuePercent;

    @Schema(description = "Minimum due fixed amount", example = "1000.00")
    private BigDecimal minimumDueAmount;

    @Schema(description = "Late fee amount", example = "750.00")
    private BigDecimal lateFeeAmount;

    @Schema(description = "Overlimit fee", example = "500.00")
    private BigDecimal overlimitFee;

    @Schema(description = "Joining fee", example = "1999.00")
    private BigDecimal joiningFee;

    @Schema(description = "Foreign transaction fee (%)",
            example = "3.50")
    private BigDecimal foreignTransactionFeePercent;

    @Schema(description = "Balance transfer fee (%)",
            example = "2.50")
    private BigDecimal balanceTransferFeePercent;

    @Schema(description = "Cash advance fee (%)",
            example = "2.00")
    private BigDecimal cashAdvanceFeePercent;

    @Schema(description = "Minimum cash advance fee amount",
            example = "300.00")
    private BigDecimal cashAdvanceFeeMin;

    @Schema(description = "Effective start date",
            example = "2026-01-01")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective end date",
            example = "2030-12-31")
    private LocalDate effectiveTo;

    @Schema(description = "Current product status",
            example = "ACTIVE")
    private ProductStatus status;
}