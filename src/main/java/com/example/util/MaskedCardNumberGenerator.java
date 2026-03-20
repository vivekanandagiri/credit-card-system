package com.example.util;


import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates a cryptographically valid 16-digit card number using the Luhn Algorithm,
 * then stores it in masked format.
 *
 * =====================================================
 * HOW THE LUHN ALGORITHM WORKS
 * =====================================================
 * The Luhn algorithm (also called Mod 10) is used by all major card networks
 * (Visa, Mastercard, RuPay) to validate card numbers and catch typos.
 *
 * Steps to compute the Luhn check digit:
 *   Given a 15-digit number (BIN + random middle + last 3 digits):
 *   1. Starting from the RIGHTMOST digit, double every SECOND digit (right to left).
 *   2. If doubling produces a number > 9, subtract 9 from it.
 *   3. Sum all digits (the doubled ones and the untouched ones).
 *   4. The check digit = (10 - (sum % 10)) % 10
 *   5. Append the check digit as the 16th digit.
 *
 * Example:
 *   15-digit payload: 4 1 1 1 1 1 5 3 7 2 8 4 9 5 6
 *   Double every 2nd from right (positions 2,4,6,...):
 *     4  2  1  2  1  2  5  6  7  4  8  8  9 10  6
 *   Subtract 9 from any result > 9: 10 → 1
 *     4  2  1  2  1  2  5  6  7  4  8  8  9  1  6 = sum 67
 *   Check digit = (10 - (67 % 10)) % 10 = (10 - 7) % 10 = 3
 *   Full number: 4111115372849563  ← Luhn-valid
 *
 * =====================================================
 * CARD NUMBER STRUCTURE (16 digits)
 * =====================================================
 * [ BIN: 6 digits ] [ Random middle: 9 digits ] [ Luhn check: 1 digit ]
 *
 * BIN (Bank Identification Number) — identifies network and issuer:
 *   VISA       → 411111  (standard Visa test BIN)
 *   MASTERCARD → 512345  (standard Mastercard test BIN)
 *   RUPAY      → 607080  (standard RuPay test BIN)
 *
 * =====================================================
 * MASKED FORMAT STORED IN DB
 * =====================================================
 * Full PAN  : 4111115372849563
 * Masked    : 411111XXXXXX9563   (BIN visible, middle masked, last 4 visible)
 *
 * Full PAN is NEVER stored in the database. Only the masked version is persisted.
 * In production, the full PAN would be handled by a PCI-DSS compliant card vault.
 */
@Component
public class MaskedCardNumberGenerator {

    // SecureRandom for cryptographically strong random numbers
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // BINs per network (Bank Identification Numbers)
    private static final String VISA_BIN       = "411111";
    private static final String MASTERCARD_BIN = "512345";
    private static final String RUPAY_BIN      = "607080";

    /**
     * Generates a Luhn-valid 16-digit card number and returns it in masked format.
     *
     * @param networkType VISA, MASTERCARD, or RUPAY
     * @return masked card number e.g. "411111XXXXXX9563"
     */
    public String generate(String networkType) {

        String bin = resolveBin(networkType);

        // Generate 9 random middle digits (positions 7–15)
        // Position 16 will be the Luhn check digit
        String middleDigits = generateRandomDigits(9);

        // Combine BIN + middle to form 15-digit payload
        String fifteenDigits = bin + middleDigits;

        // Compute the 16th digit using Luhn
        int checkDigit = computeLuhnCheckDigit(fifteenDigits);

        // Full 16-digit PAN
        String fullPan = fifteenDigits + checkDigit;

        // Mask: keep first 6 (BIN) + 6 X's + last 4
        String last4 = fullPan.substring(12);
        return bin + "XXXXXX" + last4;
    }

    // =====================================================
    // LUHN CHECK DIGIT COMPUTATION
    // =====================================================

    /**
     * Computes the Luhn check digit for a given numeric string.
     *
     * Algorithm:
     *   1. Process digits right to left.
     *   2. Double every digit at an even position from the right (1-indexed).
     *   3. If the doubled value exceeds 9, subtract 9.
     *   4. Sum all resulting digits.
     *   5. Check digit = (10 - (sum % 10)) % 10
     *
     * @param digits 15-digit string (BIN + random middle)
     * @return single check digit (0–9)
     */
    private int computeLuhnCheckDigit(String digits) {

        int sum = 0;
        boolean doubleIt = true; // rightmost digit of the 15-char payload is at even position

        // Process right to left
        for (int i = digits.length() - 1; i >= 0; i--) {

            int digit = digits.charAt(i) - '0';

            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleIt = !doubleIt;
        }

        return (10 - (sum % 10)) % 10;
    }

    /**
     * Validates whether a full card number passes the Luhn check.
     * Useful for testing and debugging.
     *
     * @param cardNumber full 16-digit card number (unmasked)
     * @return true if Luhn-valid
     */
    public boolean isLuhnValid(String cardNumber) {

        int sum = 0;
        boolean doubleIt = false; // start from rightmost digit — do NOT double it

        for (int i = cardNumber.length() - 1; i >= 0; i--) {

            int digit = cardNumber.charAt(i) - '0';

            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleIt = !doubleIt;
        }

        return (sum % 10) == 0;
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private String resolveBin(String networkType) {
        return switch (networkType.toUpperCase()) {
            case "VISA"       -> VISA_BIN;
            case "MASTERCARD" -> MASTERCARD_BIN;
            case "RUPAY"      -> RUPAY_BIN;
            default           -> VISA_BIN;
        };
    }

    private String generateRandomDigits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}