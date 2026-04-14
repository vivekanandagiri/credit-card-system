package com.example.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;

/**
 * Contract for the core  interest Calculation engine.
 */
public interface InterestCalculationService {

	/**
     * Calculates the total revolving interest accrued during a specific billing cycle.
     *
     * @param accountId the unique identifier of the target account
     * @param start the exact, timezone-aware start boundary of the billing cycle 
     * @param end the exact, timezone-aware end boundary of the billing cycle 
     * @param account the master account entity containing the current APR configuration
     * @param lastStatement the previous cycle's statement (for determining  Grace Period eligibility)
     * @param zone the legal time zone of the customer for day-boundary calculations
     * @return the exact monetary interest charge to be applied to the new statement
     */
    BigDecimal calculateInterest(
            UUID accountId,
            Instant start,
            Instant end,
            CreditAccount account,
            BillingStatement lastStatement,
            ZoneId zone
    );
}