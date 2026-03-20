package com.example.enums;

/**
 * Represents how a card is physically issued.
 * Decided at card issuance time — not at product configuration time.
 *
 * SEPARATE from CardType (CLASSIC, GOLD, PLATINUM) which lives
 * on CreditCardProduct and represents the product tier.
 *
 * Example:
 *   Visa Gold (CardType.GOLD) can be issued as:
 *     → CardFormat.VIRTUAL  (instant, no physical delivery)
 *     → CardFormat.PHYSICAL (printed and delivered)
 */
public enum CardFormat {
    VIRTUAL,   // digital card — instant issuance, used for online transactions
    PHYSICAL   // plastic card — printed and delivered to customer's address
}