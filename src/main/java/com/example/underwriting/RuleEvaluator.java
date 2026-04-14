package com.example.underwriting;

import com.example.entity.UnderwritingRule;
import com.example.enums.RuleOperator;
import com.example.exception.BadRequestException;
import com.example.underwriting.model.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Evaluates an {@link UnderwritingRule} against a given {@link ApplicationContext}.
 *
 * <p>This component is responsible for interpreting rule conditions and determining
 * whether a rule is satisfied for a specific application.</p>
 *
 * <p><b>Supported rule types:</b></p>
 * <ul>
 *     <li><b>String comparisons</b> — EQ (equals), NEQ (not equals)</li>
 *     <li><b>Numeric comparisons</b> — GT, GTE, LT, LTE</li>
 * </ul>
 *
 * <p><b>Examples:</b></p>
 * <ul>
 *     <li>{@code employmentType = "SALARIED"}</li>
 *     <li>{@code monthlyIncome > 30000}</li>
 *     <li>{@code creditScore >= 650}</li>
 * </ul>
 *
 * <p><b>Note:</b> Field values are dynamically resolved from {@link ApplicationContext},
 * making this evaluator flexible and rule-driven.</p>
 */
@Component
public class RuleEvaluator {


    /**
     * Evaluates whether a rule condition is satisfied for a given application context.
     *
     * <p>The evaluation flow:</p>
     * <ol>
     *     <li>Resolve field value from {@link ApplicationContext}</li>
     *     <li>Compare it with the rule threshold</li>
     *     <li>Apply operator logic</li>
     * </ol>
     *
     * @param rule the underwriting rule to evaluate
     * @param ctx  the application context containing field values
     * @return {@code true} if the rule condition is satisfied, otherwise {@code false}
     *
     * @throws BadRequestException if a numeric comparison is attempted on non-numeric data
     */
    public boolean evaluate(UnderwritingRule rule, ApplicationContext ctx) {

        String fieldValue = ctx.resolveField(rule.getFieldName());
        String threshold  = rule.getThresholdValue();
        RuleOperator operator = rule.getOperator();

        // String equality rules (EQ / NEQ) — e.g. employment_type = UNEMPLOYED
        if (operator == RuleOperator.EQ || operator == RuleOperator.NEQ) {
            boolean matches = fieldValue.equalsIgnoreCase(threshold);
            return (operator == RuleOperator.EQ) == matches;
        }

        /*
         * Numeric comparison using BigDecimal for precision.
         */
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
            throw new BadRequestException(
                    "Cannot compare non-numeric field '" + rule.getFieldName()
                            + "' with operator " + operator);
        }
    }
}