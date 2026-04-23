package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.example.entity.LedgerEntry;
import com.example.enums.EntryType;
import com.example.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.service.InterestCalculationService;

/**
 * Implementation of {@link InterestCalculationService} responsible for computing
 * interest on revolving credit accounts.
 *
 * <p><b>Current Interest Model:</b></p>
 * <ul>
 *     <li>Simple daily interest (no compounding)</li>
 *     <li>Interest accrues on outstanding balance per day</li>
 *     <li>Ledger events (debits/credits) dynamically adjust balance</li>
 * </ul>
 *
 * <p><b>Key Concepts:</b></p>
 * <ul>
 *     <li>APR is converted to Daily Periodic Rate (DPR)</li>
 *     <li>Interest-free grace period applies if previous dues are fully paid</li>
 *     <li>Interest is calculated across time segments between transactions</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InterestCalculationServiceImpl implements InterestCalculationService {
    private final LedgerEntryRepository ledgerEntryRepository;
    /**
     * Calculates interest for a billing cycle.
     *
     * @param accountId       unique identifier of the credit account
     * @param start           start timestamp of billing cycle
     * @param end             end timestamp of billing cycle
     * @param account         credit account containing APR and metadata
     * @param lastStatement   previous billing statement (used for grace period and carry-over balance)
     * @param zone            timezone for date calculations
     * @return calculated interest amount rounded to 2 decimal places
     */
    @Override
    public BigDecimal calculateInterest(
            UUID accountId,
            Instant start,
            Instant end,
            CreditAccount account,
            BillingStatement lastStatement,
            ZoneId zone)
    {

        BigDecimal dailyRate = calculateDailyRate(account.getApr());

        // 1. Grace Period Check
        if (isInterestFree(lastStatement)) {
            return BigDecimal.ZERO;
        }

        // 2. Opening Balance (unpaid previous balance)
        BigDecimal balance = BigDecimal.ZERO;

        if (lastStatement != null) {
            balance = lastStatement.getClosingBalance()
                    .subtract(lastStatement.getAmountPaid())
                    .max(BigDecimal.ZERO);
        }



        // 3. Fetch ledger entries within billing cycle
        List<LedgerEntry> entries =
                ledgerEntryRepository.findByAccountIdAndCreatedAtBetween(accountId, start, end);

        // If no transactions, apply simple interest on opening balance
        if (entries == null || entries.isEmpty()) {
            return calculateSimpleInterest(balance, dailyRate, start, end, zone);
        }

        // 4. Sort transactions chronologically
        entries.sort(Comparator.comparing(LedgerEntry::getCreatedAt));

        BigDecimal interest = BigDecimal.ZERO;
        Instant lastTime = start;

        // 5. Process each ledger event
        for (LedgerEntry entry : entries) {

            Instant entryTime = entry.getCreatedAt();

            if (entryTime.isBefore(start)) continue;

            long days = getDaysBetween(lastTime, entryTime, zone);

            // Accumulate interest for the period before this transaction
            if (days > 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
                interest = interest.add(
                        balance
                                .multiply(dailyRate)
                                .multiply(BigDecimal.valueOf(days))
                );
            }

            // Apply transaction effect
            if (entry.getEntryType() == EntryType.DEBIT) {
                balance = balance.add(entry.getAmount());
            } else {
                balance = balance.subtract(entry.getAmount());
            }

            lastTime = entryTime;
        }

        // 6. Final segment till the end of cycle
        long days = getDaysBetween(lastTime, end, zone);

        if (days > 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
            interest = interest.add(
                    balance
                            .multiply(dailyRate)
                            .multiply(BigDecimal.valueOf(days))
            );
        }

        return interest.setScale(2, RoundingMode.HALF_UP);
    }

    // ================= HELPER METHODS =================

    /**
     * Converts Annual Percentage Rate (APR) into Daily Periodic Rate (DPR).
     *
     * <p>Formula:</p>
     * <pre>
     * DPR = (APR / 100) / 365
     * </pre>
     *
     * @param apr annual percentage rate (e.g., 36 for 36%)
     * @return daily interest rate with high precision
     */
    private BigDecimal calculateDailyRate(BigDecimal apr) {
        return apr
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
    }

    /**
     * Determines whether the account qualifies for an interest-free grace period.
     *
     * <p>Grace period applies when:</p>
     * <ul>
     *     <li>No previous statement exists (new account)</li>
     *     <li>Previous statement was fully paid</li>
     * </ul>
     *
     * @param statement previous billing statement
     * @return true if interest should NOT be charged
     */
    private boolean isInterestFree(BillingStatement statement) {
        // If there is no previous statement, it's their first month! They get a grace period.
        if (statement == null) {
            return true;
        }
        return statement.getAmountPaid().compareTo(statement.getTotalAmountDue()) >= 0;
    }


    /**
     * Determines whether the account qualifies for an interest-free grace period.
     *
     * <p>Grace period applies when:</p>
     * <ul>
     *     <li>No previous statement exists (new account)</li>
     *     <li>Previous statement was fully paid</li>
     * </ul>
     *
     * @param statement previous billing statement
     * @return true if interest should NOT be charged
     */
    private BigDecimal calculateSimpleInterest(
            BigDecimal balance,
            BigDecimal dailyRate,
            Instant start,
            Instant end,
            ZoneId zone
    ) {
        long days = getDaysBetween(start, end, zone);

        if (days <= 0 || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return balance
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(days));
    }

    // ================= UTIL METHODS =================

    /**
     * Calculates number of full days between two instants using a timezone.
     *
     * @param start start timestamp
     * @param end   end timestamp
     * @param zone  timezone
     * @return number of days between dates
     */
    private long getDaysBetween(Instant start, Instant end, ZoneId zone) {
        LocalDate startDate = toLocalDate(start, zone);
        LocalDate endDate = toLocalDate(end, zone);
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Converts an {@link Instant} to {@link LocalDate} using given timezone.
     *
     * @param instant timestamp
     * @param zone    timezone
     * @return local date representation
     */
    private LocalDate toLocalDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}