package com.example.underwriting.model;

import com.example.entity.CreditCardApplication;
import com.example.entity.CreditProduct;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Internal model that carries all computed fields
 * through the underwriting pipeline.
 *
 * Fields available to rules via resolveField():
 *
 *   Raw fields (direct from application):
 *     credit_score_at_application
 *     monthly_income
 *     existing_liabilities
 *     requested_credit_limit
 *     employment_type
 *
 *   Derived / computed fields:
 *     annual_income         = monthly_income * 12
 *     debt_burden_ratio     = existing_liabilities / monthly_income
 *     limit_to_income_ratio = requested_credit_limit / annual_income
 */
@Getter
@Setter
public class ApplicationContext {

    private CreditCardApplication application;
    private CreditProduct creditProduct;
    
    // Derived fields
    private BigDecimal annualIncome;
    private BigDecimal debtBurdenRatio;
    private BigDecimal limitToIncomeRatio;

    public static ApplicationContext from(CreditCardApplication application) {

        ApplicationContext context = new ApplicationContext();
        context.setApplication(application);
        context.setCreditProduct(application.getCreditProduct());

        BigDecimal monthlyIncome  = application.getMonthlyIncome();
        BigDecimal liabilities    = application.getExistingLiabilities();
        BigDecimal requestedLimit = application.getRequestedCreditLimit();

        // annual_income = monthly_income * 12
        context.setAnnualIncome(
                monthlyIncome.multiply(BigDecimal.valueOf(12)));

        // debt_burden_ratio = existing_liabilities / monthly_income
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
        	context.setDebtBurdenRatio(
                    liabilities.divide(monthlyIncome, 4, RoundingMode.HALF_UP));
        } else {
        	context.setDebtBurdenRatio(BigDecimal.ZERO);
        }

        // limit_to_income_ratio = requested_limit / annual_income
        if (context.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
        	context.setLimitToIncomeRatio(
                    requestedLimit.divide(context.getAnnualIncome(), 4, RoundingMode.HALF_UP));
        } else {
        	context.setLimitToIncomeRatio(BigDecimal.ZERO);
        }

        return context;
    }

    /**
     * Resolves a named field value as a String for rule evaluation.
     * RuleEvaluator calls this to get the actual value of any field by name.
     * Add new field mappings here when new rules reference new fields.
     */
    public String resolveField(String fieldName) {
        return switch (fieldName) {

            // ── Raw application fields ──
            case "credit_score_at_application" ->
                    String.valueOf(application.getCreditScoreAtApplication());

            case "monthly_income" ->
                    application.getMonthlyIncome().toPlainString();

            case "existing_liabilities" ->
                    application.getExistingLiabilities().toPlainString();

            case "requested_credit_limit" ->
                    application.getRequestedCreditLimit().toPlainString();

            case "employment_type" ->
                    application.getEmploymentType().name();

            // ── Derived / computed fields ──
            case "annual_income" ->
                    annualIncome.toPlainString();

            case "debt_burden_ratio" ->
                    debtBurdenRatio.toPlainString();

            case "limit_to_income_ratio" ->
                    limitToIncomeRatio.toPlainString();

            default ->
                    throw new RuntimeException(
                            "Unknown rule field: '" + fieldName + "'. "
                                    + "Add it to ApplicationContext.resolveField()");
        };
    }
}