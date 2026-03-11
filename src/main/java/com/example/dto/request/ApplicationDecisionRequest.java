package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplicationDecisionRequest {

    private boolean approved;
    private String decisionReason;

    // Only required if approved = true
    private BigDecimal approvedCreditLimit;
    private BigDecimal approvedApr;
}