package com.example.underwriting;

import com.example.entity.CreditCardApplication;
import com.example.entity.UnderwritingRule;
import com.example.enums.RuleAction;
import com.example.enums.RuleType;
import com.example.repository.UnderwritingRuleRepository;
import com.example.underwriting.model.ApplicationContext;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Core brain of the underwriting system.
 *
 * Pipeline:
 * 1. Build ApplicationContext (compute derived fields)
 * 2. Load active rules from DB by type
 * 3. Evaluate ELIGIBILITY rules  → hard reject on first failure
 * 4. Evaluate RISK_SCORING rules → compute risk score
 * 5. Evaluate FRAUD rules        → flag for manual review if triggered
 * 6. Pass everything to DecisionEngine → returns UnderwritingDecision
 *
 * NOTE: This service only computes and returns the decision.
 * Persisting the result back to credit_card_applications
 * is the responsibility of CreditCardApplicationServiceImpl.
 */
@Service
@Transactional
public class UnderwritingService {

    private final UnderwritingRuleRepository ruleRepository;
    private final RuleEvaluator ruleEvaluator;
    private final RiskScoringService riskScoringService;
    private final DecisionEngine decisionEngine;

    public UnderwritingService(UnderwritingRuleRepository ruleRepository,
                               RuleEvaluator ruleEvaluator,
                               RiskScoringService riskScoringService,
                               DecisionEngine decisionEngine) {
        this.ruleRepository   = ruleRepository;
        this.ruleEvaluator    = ruleEvaluator;
        this.riskScoringService = riskScoringService;
        this.decisionEngine   = decisionEngine;
    }

    // =====================================================
    // MAIN ENTRY POINT
    // =====================================================
    public UnderwritingDecision evaluate(CreditCardApplication application) {

        // Step 1 — Build context with all derived fields
        ApplicationContext ctx = ApplicationContext.from(application);

        // Step 2 — Load active rules by type (ordered by priority ASC)
        List<UnderwritingRule> eligibilityRules =
                ruleRepository.findAllByRuleTypeAndIsActiveTrueOrderByPriorityAsc(
                        RuleType.ELIGIBILITY);

        List<UnderwritingRule> scoringRules =
                ruleRepository.findAllByRuleTypeAndIsActiveTrueOrderByPriorityAsc(
                        RuleType.RISK_SCORING);

        List<UnderwritingRule> fraudRules =
                ruleRepository.findAllByRuleTypeAndIsActiveTrueOrderByPriorityAsc(
                        RuleType.FRAUD);

        List<String> appliedRules = new ArrayList<>();

        // Step 3 — ELIGIBILITY: hard stop on first REJECT
        boolean hardRejected    = false;
        String  rejectionReason = null;

        for (UnderwritingRule rule : eligibilityRules) {
            if (ruleEvaluator.evaluate(rule, ctx)) {
                appliedRules.add("[ELIGIBILITY] " + rule.getRuleName());

                if (rule.getAction() == RuleAction.REJECT) {
                    hardRejected    = true;
                    rejectionReason = "Rejected by rule: " + rule.getRuleName();
                    break;
                }
            }
        }

        // Step 4 — RISK SCORING: accumulate score (with group exclusivity)
        BigDecimal riskScore = riskScoringService
                .calculateRiskScore(ctx, scoringRules, ruleEvaluator);

        for (UnderwritingRule rule : scoringRules) {
            if (ruleEvaluator.evaluate(rule, ctx)) {
                appliedRules.add("[SCORING] " + rule.getRuleName()
                        + " (impact: " + rule.getScoreImpact() + ")");
            }
        }

        // Step 5 — FRAUD: flag for manual review if triggered
        boolean flaggedReview = false;

        for (UnderwritingRule rule : fraudRules) {
            if (ruleEvaluator.evaluate(rule, ctx)) {
                appliedRules.add("[FRAUD] " + rule.getRuleName());

                if (rule.getAction() == RuleAction.FLAG_REVIEW) {
                    flaggedReview = true;
                }
            }
        }

        // Step 6 — Return final decision (caller handles persistence)
        return decisionEngine.decide(
                ctx, riskScore, flaggedReview, hardRejected, rejectionReason, appliedRules);
    }
}