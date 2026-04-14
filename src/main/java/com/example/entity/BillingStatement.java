package com.example.entity;

import com.example.enums.StatementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "billing_statements")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class BillingStatement extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "statement_id")
    private UUID statementId;

    // Account this statement belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private CreditAccount account;

    // ── Billing period ──
    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;   // day after last statement (or account activation)

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;     // the statement_cycle_day this month

    // ── Balances ──
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;      // currentBalance at start of period

    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal interestCharged;
    private BigDecimal remainingAmount;
    private BigDecimal lateFee;   

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal closingBalance;      // openingBalance + totalTransactions

    @Column(name = "total_amount_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmountDue;      

    // ── Minimum due ──
    
    @Column(name = "minimum_due_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumDueAmount;

    @Column(name = "min_due_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minDuePercent;       // e.g. 5.00 (%)

    @Column(name = "min_due_floor", nullable = false, precision = 19, scale = 4)
    private BigDecimal minDueFloor;         // e.g. 200.00 (₹)

    // ── Due date ──
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;              // billingPeriodEnd + grace days

    // ── Payment tracking ──
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @Column(name = "late_fee_applied", nullable = false)
    private Boolean lateFeeApplied = false;

    @Column(name = "late_fee_applied_at")
    private Instant lateFeeAppliedAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "statement_status", nullable = false,columnDefinition = "statement_status_enum")
    private StatementStatus statementStatus;

    // ── Timestamps ──
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}