package com.example.entity;

import com.example.enums.Currency;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "transactions")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "transaction_id")
    private UUID transactionId;

    // ── Relationships ──

    // Card used for this transaction 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCard card;

    // Account the transaction is posted against 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private CreditAccount account;
    
    //Payment Id for the bill payment transaction tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    
    //Bill 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id")
    private BillingStatement statement;

    // ── Transaction details ──
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "transaction_type", nullable = false,columnDefinition = "transaction_type_enum")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "transaction_status", nullable = false,columnDefinition = "transaction_status_enum")
    private TransactionStatus transactionStatus;
    
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "transaction_channel", nullable = false,columnDefinition = "transaction_channel_enum")
    private TransactionChannel transactionChannel;


    // ── Amount ──
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "currency", nullable = false, length = 3,columnDefinition = "currency_enum")
    private Currency currency;

    // ── Merchant info ──
    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    // Standard 4-digit MCC code e.g. "5411" (Grocery Stores)
    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    // Human-readable label e.g. "GROCERY_STORES"
    @Column(name = "merchant_category_name", length = 100)
    private String merchantCategoryName;

   
    // Captures account.availableBalance before and after this transaction
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    // ── Decline info ──
    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    // ── Transaction Reference Number ──
    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    private String referenceNumber;
    //Transaction Reference for purchase 
    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    // ── Time stamp ──
    @Column(name = "transaction_time", nullable = false)
    private Instant transactionTime;
}