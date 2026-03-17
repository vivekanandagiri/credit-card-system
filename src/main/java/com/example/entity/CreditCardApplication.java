package com.example.entity;

import com.example.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "credit_card_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreditCardApplication extends BaseEntity {

    @Id @GeneratedValue
    @Column(name = "application_id")
    private UUID applicationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_product_id", nullable = false)
    private CreditProduct creditProduct;

    // Employment
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "employment_type", nullable = false,columnDefinition = "employment_type_enum")
    private EmploymentType employmentType;

    @Column(name = "employer_name")
    private String employerName;

    // Financials
    @Column(name = "monthly_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyIncome;

    @Column(name = "existing_liabilities", nullable = false, precision = 19, scale = 4)
    private BigDecimal existingLiabilities;

    @Column(name = "credit_score_at_application", nullable = false)
    private Integer creditScoreAtApplication;

    @Column(name = "requested_credit_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedCreditLimit;

    // Decision fields
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "application_status", nullable = false,columnDefinition = "application_status_enum")
    private ApplicationStatus applicationStatus;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "decision", columnDefinition = "decision_type_enum")
    private DecisionType decision;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "approved_credit_limit", precision = 19, scale = 4)
    private BigDecimal approvedCreditLimit;

    @Column(name = "approved_apr", precision = 5, scale = 2)
    private BigDecimal approvedApr;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "decision_at")
    private Instant decisionAt;
}