package com.example.underwriting;

import com.example.enums.DecisionType;
import com.example.underwriting.model.ApplicationContext;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Decision engine responsible for converting a risk score and rule outcomes
 * into a final underwriting decision.
 *
 * <p>This component encapsulates the final step of the underwriting pipeline.</p>
 *
 * <p><b>Decision Flow Priority:</b></p>
 * <ol>
 *     <li><b>Hard Rejection</b> → Immediate AUTO_REJECTED</li>
 *     <li><b>Fraud Flag</b> → Forced PENDING_REVIEW</li>
 *     <li><b>Score-Based Decision</b> → APPROVED / REVIEW / REJECT</li>
 * </ol>
 *
 * <p><b>Score Thresholds:</b></p>
 * <ul>
 *     <li>{@code score >= 80}  → AUTO_APPROVED</li>
 *     <li>{@code 60 <= score < 80} → PENDING_REVIEW</li>
 *     <li>{@code score < 60}   → AUTO_REJECTED</li>
 * </ul>
 *
 * <p><b>APR Adjustment (for approved applications):</b></p>
 * <ul>
 *     <li>{@code score >= 95} → base APR - 1.5%</li>
 *     <li>{@code score >= 90} → base APR - 2.0%</li>
 *     <li>{@code score >= 85} → base APR - 1.0%</li>
 *     <li>{@code score < 85}  → no discount</li>
 * </ul>
 *
 * <p><b>Credit Limit Adjustment:</b></p>
 * <ul>
 *     <li>{@code score >= 95} → 100% of max limit</li>
 *     <li>{@code score >= 90} → 90% of max limit</li>
 *     <li>{@code score >= 85} → 75% of max limit</li>
 *     <li>{@code score < 85}  → 60% of max limit</li>
 * </ul>
 *
 * <p>Final values are clamped between product min/max limits
 * and customer's requested credit limit.</p>
 */
@Component
public class DecisionEngine {

    /**
     * Minimum score required for automatic approval.
     */
	private static final BigDecimal SCORE_AUTO_APPROVE  = new BigDecimal("80");
    /**
     * Minimum score required for manual review.
     */
    private static final BigDecimal SCORE_PENDING_REVIEW = new BigDecimal("60");

    /**
     * Produces the final underwriting decision.
     *
     * @param context         enriched application context
     * @param riskScore       calculated risk score (0–100)
     * @param flaggedReview   true if fraud rules triggered review
     * @param hardRejected    true if eligibility rules caused rejection
     * @param rejectionReason reason for rejection (if any)
     * @param appliedRules    list of rules that were triggered (audit trail)
     * @return final {@link UnderwritingDecision}
     */
    
    public UnderwritingDecision decide(ApplicationContext context,
                                       BigDecimal riskScore,
                                       boolean flaggedReview,
                                       boolean hardRejected,
                                       String rejectionReason,
                                       List<String> appliedRules) {

        // 1. Hard rejection (highest priority)
    	if (hardRejected) {
            return UnderwritingDecision.builder()
                    .decision(DecisionType.AUTO_REJECTED)
                    .riskScore(riskScore)
                    .decisionReason(rejectionReason)
                    .appliedRules(appliedRules)
                    .build();
        }

        // 2. Fraud-triggered manual review
    	if (flaggedReview) {
            return UnderwritingDecision.builder()
                    .decision(DecisionType.PENDING_REVIEW)
                    .riskScore(riskScore)
                    .decisionReason("Flagged for manual review due to fraud indicators")
                    .appliedRules(appliedRules)
                    .build();
        }

        // 3. Score-based decision
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

        // 4. Default: reject
    	 return UnderwritingDecision.builder()
                 .decision(DecisionType.AUTO_REJECTED)
                 .riskScore(riskScore)
                 .decisionReason("Automatically rejected with risk score " + riskScore)
                 .appliedRules(appliedRules)
                 .build();
    }


    /**
     * Computes the approved APR (Annual Percentage Rate).
     *
     * <p>APR represents the yearly cost of borrowing.</p>
     *
     * @param context   application context containing product details
     * @param riskScore calculated risk score
     * @return adjusted APR value
     */
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


    /**
     * Computes the approved credit limit based on risk score and constraints.
     *
     * <p>Limit is determined using:</p>
     * <ul>
     *     <li>Score-based multiplier</li>
     *     <li>Product max/min limits</li>
     *     <li>Customer requested limit</li>
     * </ul>
     *
     * @param context   application context
     * @param riskScore calculated risk score
     * @return approved credit limit
     */
    
    private BigDecimal computeApprovedLimit(ApplicationContext context,BigDecimal riskScore) {

        var creditProduct = context.getCreditProduct();
        BigDecimal requested = context.getApplication().getRequestedCreditLimit();
        BigDecimal maxCreditLimit=creditProduct.getMaxCreditLimit();
        BigDecimal minCreditLimit = creditProduct.getMinCreditLimit();

        BigDecimal adjustedMax = getBigDecimal(riskScore, maxCreditLimit);

        return requested
                .min(adjustedMax)
                .max(minCreditLimit)
                .setScale(2, RoundingMode.HALF_UP);  
    }

    private static BigDecimal getBigDecimal(BigDecimal riskScore, BigDecimal maxCreditLimit) {
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
        return adjustedMax;
    }
}