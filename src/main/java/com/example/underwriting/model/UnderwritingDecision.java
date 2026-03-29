package com.example.underwriting.model;

import com.example.enums.DecisionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result object returned by the UnderwritingService.
 * Carries the final decision, risk score, and reasoning.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingDecision {

    private DecisionType decision;
    private BigDecimal riskScore;
    private BigDecimal approvedLimit;
    private BigDecimal approvedApr;
    private String decisionReason;
    private List<String> appliedRules;  // audit trail of which rules fired
}