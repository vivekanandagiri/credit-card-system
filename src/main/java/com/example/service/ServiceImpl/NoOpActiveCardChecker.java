package com.example.service.ServiceImpl;

import com.example.service.ActiveAccountChecker;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * No-op implementation of ActiveCardChecker.
 *
 * Active until the card issuance module is built.
 * Always returns false — no cards exist yet so no customer
 * can be blocked by the active card gate.
 *
 * HOW TO SWITCH:
 * Once IssuedCardActiveCardChecker is ready:
 * 1. Remove @Primary from this class
 * 2. Add @Primary to IssuedCardActiveCardChecker
 * No other code changes needed.
 */
//Now it's disabled 
@Component
public class NoOpActiveCardChecker implements ActiveAccountChecker {

    @Override
    public boolean hasActiveAccount(UUID customerId, Long creditProductId) {
        // Account module not built yet — no active cards can exist
        return false;
    }
}