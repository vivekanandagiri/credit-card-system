package com.example.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity{
	// Statement being paid
    @Id
    @GeneratedValue
    @Column(name = "payment_id")
    private UUID paymentId;

    //Payment Account
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private CreditAccount account;

    // Payment amount
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    //Payment Method 
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // External reference (gateway txn id)
    @Column(name = "reference_id", nullable = false, unique = true)
    private String referenceId;

    //Payment Status 
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus status;

    //Payment time 
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
	
    //Prevents orphan allocation rows if allocations are removed/updated.
    @Builder.Default
    @OneToMany(
        mappedBy = "payment",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<PaymentAllocation> allocations = new ArrayList<>();

}
