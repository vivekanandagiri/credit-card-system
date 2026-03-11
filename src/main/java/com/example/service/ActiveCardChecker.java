package com.example.service;

import java.util.UUID;

/**
 * Contract for checking whether a customer already holds
 * an active card for a given card product.
 *
 * Used in Gate 8 of CreditCardApplicationServiceImpl
 * to block re-application when a live card already exists.
 *
 * TWO IMPLEMENTATIONS:
 *
 * 1. NoOpActiveCardChecker (active now)
 *    — Used until the card issuance module is built.
 *    — Always returns false (no cards exist yet).
 *    — Registered as @Primary so Spring picks it automatically.
 *
 * 2. IssuedCardActiveCardChecker (wire when card module is ready)
 *    — Queries the issued_cards table via IssuedCardRepository.
 *    — Remove @Primary from NoOpActiveCardChecker and add it here.
 */
public interface ActiveCardChecker {

    /**
     * Returns true if the customer already has an active card
     * for the given card product.
     *
     * @param customerId    UUID of the customer
     * @param cardProductId UUID of the card product being applied for
     * @return true if active card exists, false otherwise
     */
    boolean hasActiveCard(UUID customerId, UUID cardProductId);
}