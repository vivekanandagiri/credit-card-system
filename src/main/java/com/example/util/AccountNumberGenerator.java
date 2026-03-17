package com.example.util;

import com.example.repository.AccountNumberSequenceRepository;
import org.springframework.stereotype.Service;

/**
 * Generates unique 12-digit account numbers.
 *
 * Format: LLLL(4) + PP(2) + SSSSSS(6) = 12 digits, all digits, no separators
 *
 *   LLLL   = Location code  (hard coded — single branch system)
 *   PP     = Product code   (first 2 digits extracted from credit product's productCode)
 *   SSSSSS = Sequence       (from PostgreSQL native sequence, zero-padded, 000001–999999)
 *
 * Example:
 *   Location  = 0534
 *   Product   = CP-001 → digits extracted → "00"
 *   Sequence  = 1 → zero-padded → "000001"
 *   Result    → "053400000001"
 *
 * Concurrency:
 *   PostgreSQL nextval() is atomic — no application locks needed.
 *   Two simultaneous account creations always get different sequence numbers.
 *
 * Sequence exhaustion:
 *   Max value is 999999. Throws RuntimeException if exhausted.
 *   Change MAXVALUE in migration and update format if more accounts needed.
 */
@Service
public class AccountNumberGenerator {

    // Hardcoded for single branch system
    // To support multiple branches, accept locationCode as a parameter
    // and create one sequence per location in the migration
    private static final String LOCATION_CODE = "0534";

    private final AccountNumberSequenceRepository sequenceRepository;

    public AccountNumberGenerator(AccountNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generates the next unique 12-digit account number.
     *
     * @param rawProductCode product code from CreditProduct (e.g. "CP-001", "01", "VISA02")
     * @return 12-digit account number string e.g. "053400000001"
     */
    public String generate(String rawProductCode) {

        String productCode = extractProductCode(rawProductCode);

        Long nextSeq = sequenceRepository.nextValue();

        if (nextSeq == null || nextSeq > 999999) {
            throw new RuntimeException(
                    "Account number sequence exhausted. Maximum 999999 accounts reached. "
                            + "Contact system administrator.");
        }

        // LLLL(4) + PP(2) + SSSSSS(6, zero-padded)
        return LOCATION_CODE
                + productCode
                + String.format("%06d", nextSeq);
    }

    /**
     * Extracts a 2-digit product code from the raw product code string.
     *
     * Rules:
     * - Takes the first 2 digit characters found in the string
     * - Zero-pads if fewer than 2 digits found
     * - Falls back to "00" if no digits at all
     *
     * Examples:
     *   "CP-001" → "00"  (first 2 digits from "001")
     *   "01"     → "01"
     *   "VISA"   → "00"  (no digits — fallback)
     *   "P2"     → "02"  (1 digit found — zero-padded)
     *   "03-GOLD"→ "03"
     */
    private String extractProductCode(String rawProductCode) {

        if (rawProductCode == null || rawProductCode.isBlank()) {
            return "00";
        }

        String digitsOnly = rawProductCode.replaceAll("[^0-9]", "");

        if (digitsOnly.length() >= 2) {
            return digitsOnly.substring(0, 2);
        } else if (digitsOnly.length() == 1) {
            return "0" + digitsOnly;
        } else {
            return "00";
        }
    }
}