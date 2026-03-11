package com.example.underwriting;

import com.example.entity.UnderwritingRule;
import com.example.enums.RuleOperator;
import com.example.underwriting.model.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Evaluates a single UnderwritingRule against an ApplicationContext.
 * Supports numeric comparison and string equality.
 */
@Component
public class RuleEvaluator {

    /**
     * Returns true if the rule condition is satisfied for this application.
     */
    public boolean evaluate(UnderwritingRule rule, ApplicationContext ctx) {

        String fieldValue = ctx.resolveField(rule.getFieldName());
        String threshold  = rule.getThresholdValue();
        RuleOperator operator = rule.getOperator();

        // String equality rules (EQ / NEQ) — e.g. employment_type = UNEMPLOYED
        if (operator == RuleOperator.EQ || operator == RuleOperator.NEQ) {
            boolean matches = fieldValue.equalsIgnoreCase(threshold);
            return operator == RuleOperator.EQ ? matches : !matches;
        }

        // Numeric comparison rules
        try {
            BigDecimal fieldNum     = new BigDecimal(fieldValue);
            BigDecimal thresholdNum = new BigDecimal(threshold);
            int cmp = fieldNum.compareTo(thresholdNum);

            return switch (operator) {
                case GT  -> cmp > 0;
                case GTE -> cmp >= 0;
                case LT  -> cmp < 0;
                case LTE -> cmp <= 0;
                default  -> false;
            };
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Cannot compare non-numeric field '" + rule.getFieldName()
                            + "' with operator " + operator);
        }
    }
}