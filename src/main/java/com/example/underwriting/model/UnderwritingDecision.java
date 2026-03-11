package com.example.underwriting.model;

import com.example.enums.DecisionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result object returned by the UnderwritingService.
 * Carries the final decision, risk score, and reasoning.
 */
@Getter
@Builder
public class UnderwritingDecision {

    private DecisionType decision;
    private BigDecimal riskScore;
    private BigDecimal approvedLimit;
    private BigDecimal approvedApr;
    private String decisionReason;
    private List<String> appliedRules;  // audit trail of which rules fired
}