package com.example.entity;

import com.example.enums.CardFormat;
import com.example.enums.CardIssuanceReason;
import com.example.enums.CardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "cards")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class CreditCard extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "card_id")
    private UUID cardId;

    // ── Relationships ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private CreditAccount creditAccount;

    // CardProduct chosen at issuance time (e.g. Visa Gold, Mastercard Classic)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_product_id", nullable = false)
    private CreditCardProduct cardProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Card format: VIRTUAL or PHYSICAL — decided at issuance 
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "card_format", nullable = false,columnDefinition = "card_format_enum")
    private CardFormat cardFormat;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "card_status", nullable = false,columnDefinition = "card_status_enum")
    private CardStatus cardStatus;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "issuance_reason", nullable = false,columnDefinition = "card_issuance_reason_enum")
    private CardIssuanceReason issuanceReason;

   
    // Format: 411111XXXXXX1234
    @Column(name = "masked_card_number", nullable = false, unique = true, length = 19)
    private String maskedCardNumber;

    // Validity
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    // Life cycle timestamps
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Who triggered issuance — "CUSTOMER" or "ADMIN"
    @Column(name = "issued_by", nullable = false, length = 100)
    private String issuedBy;
}