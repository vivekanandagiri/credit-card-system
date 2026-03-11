package com.example.entity;

import com.example.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "credit_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_product_id")
    private Long creditProductId;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    // =====================================================
    // CREDIT LIMITS
    // =====================================================
    @Column(name = "min_credit_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal minCreditLimit;

    @Column(name = "max_credit_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxCreditLimit;

    // =====================================================
    // ELIGIBILITY
    // =====================================================
    @Column(name = "min_income_required", nullable = false, precision = 19, scale = 4)
    private BigDecimal minIncomeRequired;

    @Column(name = "min_credit_score", nullable = false)
    private Integer minCreditScore;

    // =====================================================
    // APR & INTEREST
    // =====================================================
    @Column(name = "apr_purchase", nullable = false, precision = 5, scale = 2)
    private BigDecimal aprPurchase;

    @Column(name = "apr_cash_advance", nullable = false, precision = 5, scale = 2)
    private BigDecimal aprCashAdvance;

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays;

    @Column(name = "interest_calculation_method", nullable = false)
    private String interestCalculationMethod;

    // =====================================================
    // MINIMUM DUE
    // =====================================================
    @Column(name = "minimum_due_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumDuePercent;

    @Column(name = "minimum_due_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumDueAmount;

    // =====================================================
    // FEES
    // =====================================================
    @Column(name = "late_fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lateFeeAmount;

    @Column(name = "overlimit_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal overlimitFee;

    @Column(name = "joining_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal joiningFee;

    @Column(name = "foreign_transaction_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal foreignTransactionFeePercent;

    @Column(name = "balance_transfer_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal balanceTransferFeePercent;

    @Column(name = "cash_advance_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cashAdvanceFeePercent;

    @Column(name = "cash_advance_fee_min", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashAdvanceFeeMin;

    // =====================================================
    // VALIDITY & STATUS
    // =====================================================
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false,columnDefinition = "product_status_enum")
    private ProductStatus status;

    // =====================================================
    // RELATIONSHIP
    // =====================================================
    @OneToMany(mappedBy = "creditProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CreditCardProduct> cardProducts;
}