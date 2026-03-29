package com.example.service.ServiceImpl;

import com.example.service.ActiveAccountChecker;
import com.example.service.CreditAccountService;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real implementation of ActiveCardChecker.
 *
 * Queries the accounts table to check whether the customer
 * already holds an ACTIVE account for the given card product.
 *
 * TO ACTIVATE:
 * 1. Remove @Primary from NoOpActiveCardChecker
 * 2. Add @Primary to this class (already done below)
 * 3. NoOpActiveCardChecker becomes unused — can be deleted
 */
@Primary
@Component
public class IssuedCardActiveCardChecker implements ActiveAccountChecker {

	private final CreditAccountService creditAccountService;

    public IssuedCardActiveCardChecker(CreditAccountService creditAccountService) {
        this.creditAccountService = creditAccountService;
    }

    @Override
    public boolean hasActiveAccount(UUID customerId,Long creditProductId) {
        return creditAccountService.hasActiveAccountForProduct(customerId, creditProductId);
    }
}