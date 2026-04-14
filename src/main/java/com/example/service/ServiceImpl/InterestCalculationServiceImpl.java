package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.entity.Transaction;
import com.example.enums.TransactionType;
import com.example.repository.TransactionRepository;
import com.example.service.InterestCalculationService;

/**
 * Core financial engine for calculating revolving credit interest.
 * CURRENT MATH MODEL: Simple Interest on Previous Balance + New Purchases.
 */
@Service
@RequiredArgsConstructor
public class InterestCalculationServiceImpl implements InterestCalculationService {
    private final TransactionRepository transactionRepository;
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

        // 1. GRACE PERIOD 
        // If the customer paid their last statement in full, they are in their "Grace Period" 
        // and accrue absolutely zero interest on new purchases.
        if (isInterestFree(lastStatement)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalInterest = BigDecimal.ZERO;

        // 2. Interest on unpaid previous balance
        totalInterest = totalInterest.add(
                calculatePreviousBalanceInterest(start, end, lastStatement, dailyRate, zone)
        );

        // 3. Interest on new transactions
        totalInterest = totalInterest.add(
                calculateTransactionInterest(accountId, start, end, lastStatement, dailyRate, zone)
        );

        return totalInterest.setScale(2, RoundingMode.HALF_UP);
        //return BigDecimal.ZERO;
    }

    // ================= HELPER METHODS =================

    /**
     * Converts Annual Percentage Rate (APR) to the Daily Periodic Rate (DPR).
     * Formula: DPR = (APR / 100) / 365
     */
    private BigDecimal calculateDailyRate(BigDecimal apr) {
        return apr
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
    }

    /**
     * Determines if the account has maintained its interest-free grace period.
     */
    private boolean isInterestFree(BillingStatement statement) {
        // If there is no previous statement, it's their first month! They get a grace period.
        if (statement == null) {
            return true;
        }
        return statement.getAmountPaid().compareTo(statement.getTotalAmountDue()) >= 0;
    }

    /**
     * Calculates interest accrued on the revolving balance carried over from the previous month.
     */
    private BigDecimal calculatePreviousBalanceInterest(
            Instant start,
            Instant end,
            BillingStatement statement,
            BigDecimal dailyRate,
            ZoneId zone) {
    	
        if (statement == null) return BigDecimal.ZERO;

        BigDecimal unpaid = statement.getClosingBalance()
                .subtract(statement.getAmountPaid())
                .max(BigDecimal.ZERO);

        long days = getDaysBetween(start, end, zone);

        return unpaid.multiply(dailyRate).multiply(BigDecimal.valueOf(days));
    }

    /**
     * Calculates interest on new purchases made during the current cycle.
     * Interest begins accruing on the exact day the transaction is posted.
     */
    private BigDecimal calculateTransactionInterest(
            UUID accountId,
            Instant start,
            Instant end,
            BillingStatement lastStatement,
            BigDecimal dailyRate,
            ZoneId zone
    ) {

        Instant cutoff = getLastStatementEnd(lastStatement, start, zone);

        List<Transaction> transactions = Optional.ofNullable(
                transactionRepository.findTransactionsForInterest(
                        accountId, start, end, TransactionType.PURCHASE
                )
        ).orElse(List.of());

        LocalDate endDate = toLocalDate(end, zone);

        BigDecimal interest = BigDecimal.ZERO;

        for (Transaction txn : transactions) {

            if (txn.getTransactionTime().isBefore(cutoff)) {
                continue;// Already accounted for in previous balance
            }

            LocalDate txnDate = toLocalDate(txn.getTransactionTime(), zone);
            long days = ChronoUnit.DAYS.between(txnDate, endDate);

            if (days <= 0) continue;
            // Calculates the interest from the day of purchase to the end of the billing cycle
            interest = interest.add(
                    txn.getAmount()
                            .multiply(dailyRate)
                            .multiply(BigDecimal.valueOf(days))
            );
        }

        return interest;
    }

    // ================= UTIL METHODS =================

    private Instant getLastStatementEnd(
            BillingStatement statement,
            Instant start,
            ZoneId zone
    ) {
        return (statement != null)
                ? statement.getBillingPeriodEnd().atStartOfDay(zone).toInstant()
                : start;
    }

    private long getDaysBetween(Instant start, Instant end, ZoneId zone) {
        LocalDate startDate = toLocalDate(start, zone);
        LocalDate endDate = toLocalDate(end, zone);
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    private LocalDate toLocalDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}