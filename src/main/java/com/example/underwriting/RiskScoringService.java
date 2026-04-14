package com.example.underwriting;

import com.example.entity.UnderwritingRule;
import com.example.enums.RuleAction;
import com.example.underwriting.model.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes cumulative risk score from all RISK_SCORING rules.
 *
 * Base score: 0  (applicant earns score entirely through rules)
 * Score range: 0 to 100
 * All score_impacts are POSITIVE — higher score = safer applicant.
 *
 * DecisionEngine thresholds:
 *   >= 80 → AUTO_APPROVED
 *   60-79 → PENDING_REVIEW
 *   <  60 → AUTO_REJECTED
 *
 * Rule group behaviour:
 *   rule_group = NULL  → fires independently (always evaluated)
 *   rule_group = value → only FIRST matching rule in group fires,
 *                        rest of the group is skipped.
 *
 * IMPORTANT: Within a group, rules must be ordered by priority ASC
 * with the most restrictive condition at the lowest priority number
 * so it is evaluated first.
 */
@Service
public class RiskScoringService {

    private static final BigDecimal BASE_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MIN_SCORE  = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE  = new BigDecimal("100");

    /**
     * Risk Score Calculation
     * @param ctx
     * @param scoringRules
     * @param ruleEvaluator
     * @return
     */
    public BigDecimal calculateRiskScore(ApplicationContext ctx,
                                         List<UnderwritingRule> scoringRules,
                                         RuleEvaluator ruleEvaluator) {

        BigDecimal score = BASE_SCORE;

        // Tracks which rule groups have already fired
        Set<String> firedGroups = new HashSet<>();

        for (UnderwritingRule rule : scoringRules) {  // sorted by priority ASC

            if (rule.getAction() != RuleAction.SCORE) continue;

            String group = rule.getRuleGroup();

            // Group already fired → skip this rule entirely
            if (group != null && firedGroups.contains(group)) {
                continue;
            }

            if (ruleEvaluator.evaluate(rule, ctx)) {
                score = score.add(rule.getScoreImpact());

                // Mark group as fired — no other rule in this group will run
                if (group != null) {
                    firedGroups.add(group);
                }
            }
        }

        // Clamp between 0 and 100
        return score.max(MIN_SCORE).min(MAX_SCORE);
    }
}