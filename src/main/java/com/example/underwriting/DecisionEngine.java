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
 * APR tiers (applied on AUTO_APPROVED):
 *   score >= 95 → base APR - 1.5%
 *   score >= 90 → base APR - 1.25%
 *   score >= 85 → base APR - 1 %
 *   score <  85 → base APR (no discount)
 *   
 *   Approved limit tiers (scaled by score):
 *   score >= 95 → 100% of product max limit
 *   score >= 90 →  90% of product max limit
 *   score >= 85 →  75% of product max limit
 *   score <  85 →  60% of product max limit
 *   (then clamped within product min and customer's requested limit)
 *
 * Also respects hard REJECT flags set earlier in the pipeline.
 */
@Component
public class DecisionEngine {

	private static final BigDecimal SCORE_AUTO_APPROVE  = new BigDecimal("80");
    private static final BigDecimal SCORE_PENDING_REVIEW = new BigDecimal("60");
 

    /**
     * @param ctx           Application context with all computed fields
     * @param riskScore     Final computed risk score (0-100)
     * @param flaggedReview True if any FRAUD rule triggered FLAG_REVIEW
     * @param hardRejected  True if any ELIGIBILITY rule triggered REJECT
     * @param rejectionReason Reason string if hard rejected
     * @param appliedRules  Audit trail of rules that fired
     */
    
    public UnderwritingDecision decide(ApplicationContext context,
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
    	 if (riskScore.compareTo(SCORE_AUTO_APPROVE) >= 0) {
             BigDecimal approvedApr   = computeApprovedApr(context, riskScore);
             BigDecimal approvedLimit = computeApprovedLimit(context, riskScore);
  
             return UnderwritingDecision.builder()
                     .decision(DecisionType.AUTO_APPROVED)
                     .riskScore(riskScore)
                     .approvedLimit(approvedLimit)
                     .approvedApr(approvedApr)
                     .decisionReason("Automatically approved with risk score " + riskScore)
                     .appliedRules(appliedRules)
                     .build();
         }

    	 if (riskScore.compareTo(SCORE_PENDING_REVIEW) >= 0) {
             return UnderwritingDecision.builder()
                     .decision(DecisionType.PENDING_REVIEW)
                     .riskScore(riskScore)
                     .decisionReason("Sent for manual review with risk score " + riskScore)
                     .appliedRules(appliedRules)
                     .build();
         }

        // Score below reject threshold
    	 return UnderwritingDecision.builder()
                 .decision(DecisionType.AUTO_REJECTED)
                 .riskScore(riskScore)
                 .decisionReason("Automatically rejected with risk score " + riskScore)
                 .appliedRules(appliedRules)
                 .build();
    }


    // APPROVED LIMIT CALCULATION

    
    private BigDecimal computeApprovedApr(ApplicationContext context,BigDecimal riskScore) {
    	BigDecimal baseApr = context.getCreditProduct().getAprPurchase();
    	
    	BigDecimal discount;
    	
    	if(riskScore.compareTo(new BigDecimal("95"))>=0) {
    		discount=new BigDecimal("1.50");
    	  } else if (riskScore.compareTo(new BigDecimal("90")) >= 0) {
              discount = new BigDecimal("2.00");
          } else if (riskScore.compareTo(new BigDecimal("85")) >= 0) {
              discount = new BigDecimal("1.00");
          } else {
              discount = BigDecimal.ZERO;
          }
    	return baseApr.subtract(discount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    
    
    
    
    
    private BigDecimal computeApprovedLimit(ApplicationContext context,BigDecimal riskScore) {

        var creditProduct = context.getCreditProduct();
        BigDecimal requested = context.getApplication().getRequestedCreditLimit();
        BigDecimal maxCreditLimit=creditProduct.getMaxCreditLimit();
        BigDecimal minCreditLimit = creditProduct.getMinCreditLimit();


        
        BigDecimal scoreMultiplier;
        if (riskScore.compareTo(new BigDecimal("95")) >= 0) {
            scoreMultiplier = new BigDecimal("1.00");
        } else if (riskScore.compareTo(new BigDecimal("90")) >= 0) {
            scoreMultiplier = new BigDecimal("0.90");
        } else if (riskScore.compareTo(new BigDecimal("85")) >= 0) {
            scoreMultiplier = new BigDecimal("0.75");
        } else {
            scoreMultiplier = new BigDecimal("0.60");
        }
        
        BigDecimal adjustedMax = maxCreditLimit.multiply(scoreMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        
        return requested
                .min(adjustedMax)
                .max(minCreditLimit)
                .setScale(2, RoundingMode.HALF_UP);  
    }


}