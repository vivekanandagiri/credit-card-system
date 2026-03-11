package com.example.underwriting;

import com.example.enums.DecisionType;
import com.example.underwriting.model.ApplicationContext;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Converts a risk score into a final decision.
 *
 * Score thresholds:
 *   >= 80  →  AUTO_APPROVED
 *   60-79  →  PENDING_REVIEW
 *   < 60   →  AUTO_REJECTED
 *
 * Also respects hard REJECT flags set earlier in the pipeline.
 */
@Component
public class DecisionEngine {

    private static final BigDecimal APPROVE_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal REVIEW_THRESHOLD  = new BigDecimal("60");

    /**
     * @param ctx           Application context with all computed fields
     * @param riskScore     Final computed risk score (0-100)
     * @param flaggedReview True if any FRAUD rule triggered FLAG_REVIEW
     * @param hardRejected  True if any ELIGIBILITY rule triggered REJECT
     * @param rejectionReason Reason string if hard rejected
     * @param appliedRules  Audit trail of rules that fired
     */
    public UnderwritingDecision decide(ApplicationContext ctx,
                                       BigDecimal riskScore,
                                       boolean flaggedReview,
                                       boolean hardRejected,
                                       String rejectionReason,
                                       List<String> appliedRules) {

        // Hard reject takes priority — no further scoring matters
        if (hardRejected) {
            return UnderwritingDecision.builder()
                    .decision(DecisionType.AUTO_REJECTED)
                    .riskScore(riskScore)
                    .decisionReason(rejectionReason)
                    .appliedRules(appliedRules)
                    .build();
        }

        // Fraud flag forces manual review regardless of score
        if (flaggedReview) {
            return UnderwritingDecision.builder()
                    .decision(DecisionType.PENDING_REVIEW)
                    .riskScore(riskScore)
                    .decisionReason("Flagged for manual review due to fraud indicators")
                    .appliedRules(appliedRules)
                    .build();
        }

        // Score-based decision
        if (riskScore.compareTo(APPROVE_THRESHOLD) >= 0) {

            BigDecimal approvedLimit = computeApprovedLimit(ctx);
            BigDecimal approvedApr   = computeApprovedApr(ctx, riskScore);

            return UnderwritingDecision.builder()
                    .decision(DecisionType.AUTO_APPROVED)
                    .riskScore(riskScore)
                    .approvedLimit(approvedLimit)
                    .approvedApr(approvedApr)
                    .decisionReason("Approved based on credit profile and risk score of " + riskScore)
                    .appliedRules(appliedRules)
                    .build();
        }

        if (riskScore.compareTo(REVIEW_THRESHOLD) >= 0) {
            return UnderwritingDecision.builder()
                    .decision(DecisionType.PENDING_REVIEW)
                    .riskScore(riskScore)
                    .decisionReason("Risk score " + riskScore + " requires manual review")
                    .appliedRules(appliedRules)
                    .build();
        }

        // Score below reject threshold
        return UnderwritingDecision.builder()
                .decision(DecisionType.AUTO_REJECTED)
                .riskScore(riskScore)
                .decisionReason("Risk score " + riskScore + " is below minimum acceptance threshold")
                .appliedRules(appliedRules)
                .build();
    }

    // =====================================================
    // APPROVED LIMIT CALCULATION
    // =====================================================
    private BigDecimal computeApprovedLimit(ApplicationContext ctx) {

        var creditProduct = ctx.getCardProduct().getCreditProduct();
        BigDecimal requested = ctx.getApplication().getRequestedCreditLimit();

        // Approved limit = requested, clamped within product min/max
        return requested
                .min(creditProduct.getMaxCreditLimit())
                .max(creditProduct.getMinCreditLimit());
    }

    // =====================================================
    // APR CALCULATION — higher risk score = lower APR reward
    // Score 80-89 → base APR
    // Score 90-100 → 1% discount
    // =====================================================
    private BigDecimal computeApprovedApr(ApplicationContext ctx, BigDecimal riskScore) {

        BigDecimal baseApr = ctx.getCardProduct()
                .getCreditProduct()
                .getAprPurchase();

        if (riskScore.compareTo(new BigDecimal("90")) >= 0) {
            return baseApr.subtract(new BigDecimal("1.00"))
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return baseApr.setScale(2, RoundingMode.HALF_UP);
    }
}