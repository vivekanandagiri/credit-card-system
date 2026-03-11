package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.ApplicationStatus;
import com.example.enums.DecisionType;
import com.example.enums.EmploymentType;
import com.example.enums.NetworkType;

@Data
@AllArgsConstructor
public class CreditCardApplicationResponse {

    private UUID applicationId;

    // Customer info
    private UUID customerId;
    private String customerName;

    // Card product info
    private UUID cardProductId;
    private String cardProductName;
    private NetworkType networkType;

    // Application data
    private EmploymentType employmentType;
    private String employerName;
    private BigDecimal monthlyIncome;
    private BigDecimal existingLiabilities;
    private Integer creditScoreAtApplication;
    private BigDecimal requestedCreditLimit;

    // Decision
    private ApplicationStatus applicationStatus;
    private DecisionType decision;
    private String decisionReason;
    private BigDecimal approvedCreditLimit;
    private BigDecimal approvedApr;

    // Timestamps
    private Instant submittedAt;
    private Instant decisionAt;
}