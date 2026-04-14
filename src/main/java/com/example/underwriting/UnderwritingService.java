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
 * Core underwriting engine responsible for evaluating credit card applications.
 *
 * <p>This service orchestrates the complete decision-making pipeline by:
 * <ul>
 *     <li>Building an {@link ApplicationContext} with derived attributes</li>
 *     <li>Fetching active rules from the database</li>
 *     <li>Evaluating eligibility, risk scoring, and fraud rules</li>
 *     <li>Delegating final decision to {@link DecisionEngine}</li>
 * </ul>
 *
 * <p><b>Processing Pipeline:</b></p>
 * <ol>
 *     <li><b>Context Creation</b> — Convert raw application into enriched context</li>
 *     <li><b>Eligibility Rules</b> — Hard rejection if any rule fails</li>
 *     <li><b>Risk Scoring</b> — Compute aggregate risk score</li>
 *     <li><b>Fraud Detection</b> — Flag suspicious cases for manual review</li>
 *     <li><b>Decision</b> — Final outcome determined by {@link DecisionEngine}</li>
 * </ol>
 *
 * <p><b>Important:</b> This service is stateless and does NOT persist results.
 * Persistence is handled by {@code CreditAccountApplicationServiceImpl}.</p>
 */
@Service
@Transactional
public class UnderwritingService {
	//Constructor Injection
    private final UnderwritingRuleRepository ruleRepository;
    private final RuleEvaluator ruleEvaluator;
    private final RiskScoringService riskScoringService;
    private final DecisionEngine decisionEngine;


    /**
     * Constructs the underwriting service with required dependencies.
     *
     * @param ruleRepository     repository for retrieving rules
     * @param ruleEvaluator      rule evaluation engine
     * @param riskScoringService service for computing risk score
     * @param decisionEngine     final decision engine
     */
    public UnderwritingService(UnderwritingRuleRepository ruleRepository,
                               RuleEvaluator ruleEvaluator,
                               RiskScoringService riskScoringService,
                               DecisionEngine decisionEngine) {
        this.ruleRepository   = ruleRepository;
        this.ruleEvaluator    = ruleEvaluator;
        this.riskScoringService = riskScoringService;
        this.decisionEngine   = decisionEngine;
    }


    /**
     * Evaluates a credit card application and returns an underwriting decision.
     *
     * <p>This method executes the full underwriting pipeline:</p>
     * <ul>
     *     <li>Builds application context</li>
     *     <li>Executes eligibility checks (hard rejection possible)</li>
     *     <li>Calculates risk score</li>
     *     <li>Evaluates fraud signals</li>
     *     <li>Generates final decision</li>
     * </ul>
     *
     * @param application the credit card application entity
     * @return {@link UnderwritingDecision} containing decision, score, and reasons
     */
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

        /*
         * Tracks all rules that were triggered during evaluation.
         * Useful for audit, debugging, and explainability.
         */
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