package com.example.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import com.example.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_accounts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class CreditAccount extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "account_id")
    private UUID accountId;

    // Location code (4 digit ) + Product code(2) + serial no (6) = 12 digits, unique
    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;


    // ── Relationships ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditCardApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_product_id", nullable = false)
    private CreditProduct creditProduct;

    // ── Status ──
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "account_status", nullable = false,columnDefinition = "account_status_enum")
    private AccountStatus accountStatus;

    // ── Credit terms (immutable after creation) ──
    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "apr", nullable = false, precision = 5, scale = 2)
    private BigDecimal apr;

    // ── Live financial state ──
    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays;

    @Column(name = "minimum_due_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumDuePercent;

    @Column(name = "late_fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lateFeeAmount;

    // ── Billing ──
    @Column(name = "statement_cycle_day", nullable = false)
    private Integer statementCycleDay;

    @Column(name = "last_statement_date")
    private Instant lastStatementDate;

    @Column(name = "last_statement_balance", precision = 19, scale = 4)
    private BigDecimal lastStatementBalance;

    @Column(name = "next_due_date")
    private Instant nextDueDate;

    @Column(name = "last_payment_date")
    private Instant lastPaymentDate;

    @Column(name = "last_payment_amount", precision = 19, scale = 4)
    private BigDecimal lastPaymentAmount;

    // ── Life cycle ──
    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}