package com.example.entity;

import com.example.enums.CardType;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * Represents a card product in the product catalog.
 *
 * CreditCardProduct is fully INDEPENDENT of CreditProduct.
 * They are separate catalogs with no direct FK between them.
 *
 * CreditProduct  → defines credit rules (APR, limits, eligibility)
 * CreditCardProduct → defines card features (network, limits, fees)
 *
 * They are linked at runtime through:
 *   Account → CreditProduct  (credit rules for the account)
 *   Card    → CreditCardProduct (card features for each card)
 *
 * This allows the same account to have:
 *   Card 1 → Visa Platinum
 *   Card 2 → RuPay Classic
 * Both sharing the same credit limit and billing cycle.
 */
@Entity
@Table(name = "credit_card_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardProduct extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "card_product_id")
    private UUID cardProductId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "network_type", nullable = false,columnDefinition = "card_network_enum")
    private NetworkType networkType;         // VISA, MASTERCARD, RUPAY

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "card_type", nullable = false,columnDefinition = "card_type_enum")
    private CardType cardType;              // Physical,Virtual

    // FEES & VALIDITY
    @Column(name = "annual_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal annualFee;

    @Column(name = "card_validity_years", nullable = false)
    private Integer cardValidityYears;

    // FEATURES
    @Column(name = "contactless_enabled", nullable = false)
    private Boolean contactlessEnabled;

    @Column(name = "international_usage_allowed", nullable = false)
    private Boolean internationalUsageAllowed;

    @Column(name = "online_transactions_allowed", nullable = false)
    private Boolean onlineTransactionsAllowed;

    @Column(name = "atm_withdrawal_allowed", nullable = false)
    private Boolean atmWithdrawalAllowed;

    // DAILY LIMITS
    @Column(name = "atm_daily_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal atmDailyLimit;

    @Column(name = "pos_daily_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal posDailyLimit;

    @Column(name = "ecommerce_daily_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal ecommerceDailyLimit;

    // BILLING
    @Column(name = "statement_cycle_day", nullable = false)
    private Integer statementCycleDay;     // 1-28

    @Column(name = "forex_markup_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal forexMarkupPercent;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false,columnDefinition = "product_status_enum")
    private ProductStatus status;
}