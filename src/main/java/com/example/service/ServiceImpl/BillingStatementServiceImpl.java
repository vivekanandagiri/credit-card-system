package com.example.service.ServiceImpl;


import com.example.config.TimezoneResolver;
import com.example.dto.response.BillingStatementResponse;
import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.enums.StatementStatus;
import com.example.enums.TransactionType;
import com.example.exception.BadRequestException;
import com.example.mapper.BillingStatementMapper;
import com.example.repository.BillingStatementRepository;
import com.example.repository.LedgerEntryRepository;
import com.example.repository.PaymentAllocationRepository;
import com.example.service.*;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service responsible for managing credit card billing statements.
 *
 * <p>This service acts as the financial ledger engine for credit accounts,
 * handling billing cycle execution, statement generation, interest calculation,
 * payment evaluation, and delinquency handling.</p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *     <li>Generate billing statements (automated and manual)</li>
 *     <li>Ensure idempotent and timezone-aware billing execution</li>
 *     <li>Calculate balances, interest, and minimum dues</li>
 *     <li>Track payment outcomes (PAID, REVOLVING, OVERDUE)</li>
 *     <li>Apply penalties such as late fees</li>
 * </ul>
 *
 * <h3>Financial Safety Guarantees</h3>
 * <ul>
 *     <li>All monetary calculations use {@link BigDecimal}</li>
 *     <li>Operations are idempotent (safe for retries)</li>
 *     <li>Timezone-aware processing</li>
 * </ul>
 *
 * <p><b>Important:</b> This is a critical financial component.
 * Any modification must preserve ledger integrity and audit correctness.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BillingStatementServiceImpl implements BillingStatementService {

    private final BillingStatementRepository billingRepository;
    private final TransactionService transactionService;
    private final CreditAccountService accountService;
    private final BillingStatementMapper mapper;
    private final TimezoneResolver timezoneResolver;
    private final InterestCalculationService interestService;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final EntityManager entityManager;

    /**
     * Generates a billing statement for the given account.
     *
     * <p>Execution pipeline:</p>
     * <ol>
     *     <li>Validate billing cycle</li>
     *     <li>Prevent duplicate generation</li>
     *     <li>Compute balances & interest</li>
     *     <li>Persist statement</li>
     *     <li>Update account snapshot</li>
     * </ol>
     *
     * @param accountId credit account ID
     * @return generated statement response
     * @throws BadRequestException if cycle not reached or duplicate exists
     */
    @Override
    @Transactional // Override for write operation
    public BillingStatementResponse generateStatement(UUID accountId) {
        CreditAccount account = accountService.getAccountEntity(accountId);
        ZoneId zone = timezoneResolver.resolve(account.getCustomer());

        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = resolveBillingDate(
                now.toLocalDate(),
                account.getStatementCycleDay()
        );

        log.info("Initiating statement generation | accountId={} | date={}", accountId, today);

        // 1. Billing cycle validation
        validateBillingCycle(account, today);
        // 2. Prevent duplicate statement FIRST
        preventDuplicate(accountId, today);

        // 3-9. Execute full statement generation pipeline
        return executeStatementPipeline(account, zone, now, today);
    }

    /**
     * Generates a billing statement bypassing billing cycle validation.
     *
     * <p>Used by administrators for manual interventions such as:
     * <ul>
     *     <li>Customer dispute resolution</li>
     *     <li>System correction</li>
     *     <li>Testing scenarios</li>
     * </ul>
     *
     * <p>Still enforces duplicate prevention.</p>
     *
     * @param accountId credit account ID
     * @return generated billing statement
     */
    @Override
    @Transactional
    public BillingStatementResponse generateStatementManually(UUID accountId) {

        CreditAccount account = accountService.getAccountEntity(accountId);
        ZoneId zone = timezoneResolver.resolve(account.getCustomer());

        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = resolveBillingDate(
                now.toLocalDate(),
                account.getStatementCycleDay()
        );
        log.info("Manual statement generation | accountId={} | date={}", accountId, today);

        // Still prevent duplicates
        preventDuplicate(accountId, today);

        // Execute full statement generation pipeline
        return executeStatementPipeline(account, zone, now, today);
    }

    /**
     * Generates a statement for a specific date (testing/support use).
     *
     * @param accountId account ID
     * @param inputDate forced billing date
     * @return generated statement
     */
    @Transactional
    public BillingStatementResponse generateStatementForDate(UUID accountId, LocalDate inputDate) {

        CreditAccount account = accountService.getAccountEntity(accountId);
        ZoneId zone = timezoneResolver.resolve(account.getCustomer());

        ZonedDateTime now = inputDate.atStartOfDay(zone);

        LocalDate today = resolveBillingDate(
                inputDate,
                account.getStatementCycleDay()
        );

        log.info("Manual TEST statement generation | accountId={} | forcedDate={} | resolvedCycleDate={}",
                accountId, inputDate, today);

        preventDuplicate(accountId, today);

        return executeStatementPipeline(account, zone, now, today);
    }
    /**
     * Evaluates all statements whose due date has passed.
     *
     * <p>Executed by a scheduled job to determine payment outcomes:</p>
     * <ul>
     *     <li>PAID → full payment received</li>
     *     <li>REVOLVING → minimum due paid</li>
     *     <li>OVERDUE → insufficient payment</li>
     * </ul>
     *
     * <p>Also applies late fees where applicable.</p>
     */
    @Override
    @Transactional
    public void processDueStatements() {

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        List<BillingStatement> dueStatements =
                billingRepository.findDueStatementsPendingEvaluation(
                        today
                );

        log.info("Processing {} due statements for date={}", dueStatements.size(), today);

        for (BillingStatement statement : dueStatements)try {
            evaluateDueDateOutcome(statement);

        } catch (Exception ex) {
            log.error("Failed to evaluate due statement | statementId={} | error={}",
                    statement.getStatementId(), ex.getMessage(), ex);
            // Continue processing remaining statements; don't abort the entire batch.
        }
        billingRepository.saveAll(dueStatements);
    }
    /**
     * * Mark Due Statement for Reminder(e.g., 7 days before due date)
     *  @param dueReminderDays Reminder customer before the due dates
     */
    @Override
    public void markDueStatements(int dueReminderDays) {

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate threshold = today.plusDays(dueReminderDays);

        List<BillingStatement> dueSoon =
                billingRepository.findStatementsDueSoon(today, threshold);

        log.info("Found {} statements due soon", dueSoon.size());

        // Trigger notification/reminder service here instead
    }
    
    
    /**
     * Fetch a billing statement by ID.
     *
     * @param statementId statement ID
     * @return billing statement
     */
    @Override
    public BillingStatement getStatement(UUID statementId) {
        return billingRepository.findById(statementId)
                .orElseThrow(() -> new BadRequestException("Statement not found"));
    }
    
    /**
     * Fetch a billing statement with lock (for update operations).
     *
     * @param statementId statement ID
     * @return billing statement
     */
    @Override
    public BillingStatement getStatementForUpdate(UUID statementId) {
        return billingRepository.findByIdForUpdate(statementId)
                .orElseThrow(() -> new BadRequestException("Statement not found"));
    }
    
    
    /**
     * Persists updated billing statement.
     *
     * @param statement statement entity
     * @return saved entity
     */
    @Override
    public BillingStatement save(BillingStatement statement) {
        return billingRepository.save(statement);
    }

    /**
     * Retrieves all statements for an account.
     *
     * @param accountId account ID
     * @return list of statements
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingStatementResponse> getStatements(UUID accountId) {

        // Validate account exists
        // CreditAccount account = accountService.getAccountEntity(accountId);

        List<BillingStatement> statements =
                billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId);

        return statements.stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Retrieves statements for an account with ownership validation.
     *
     * @param userId    user ID
     * @param accountId account ID
     * @return list of statements
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingStatementResponse> getCustomerStatementsByAccount(
            UUID userId,
            UUID accountId) {

        // Fetch an account
        CreditAccount account = accountService.getAccountEntity(accountId);

        // Ownership validation
        if (!account.getCustomer().getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Access denied: This account does not belong to You");
        }

        // Fetch statements
        List<BillingStatement> statements =
                billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId);

        // Map to response
        return statements.stream()
                .map(mapper::toResponse)
                .toList();
    }
    
    
    //============================================= Helper Methods ====================================================
    /**
     * extracted the shared billing pipeline that was common between automated and manual generation to avoid code duplication.
     * @param account credit account
     * @param zone    customer's resolved time zone
     * @param now     current zoned date-time
     * @param today   current local date in customer's time zone
     * @return generated billing statement response
     */
    private BillingStatementResponse executeStatementPipeline(
            CreditAccount account,
            ZoneId zone,
            ZonedDateTime now,
            LocalDate today) {

        UUID accountId = account.getAccountId();

        Optional<BillingStatement> lastStatementOpt = billingRepository
                .findTopByAccountOrderByBillingPeriodEndDesc(account);
        BillingStatement lastStatement = lastStatementOpt.orElse(null);

        boolean updated = ensureLateFeeAppliedIfOverdue(lastStatement, zone);
        if (updated) {
            billingRepository.save(lastStatement);
        }
        
        LocalDate startDate = resolveStartDate(account, lastStatementOpt, zone);
        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal openingBalance = defaultZero(account.getLastStatementBalance());

        // 4. CALCULATE + POST-INTEREST FIRST
        BigDecimal interest = interestService.calculateInterest(
                accountId, start, end, account, lastStatementOpt.orElse(null), zone
        );
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            postInterestTransaction(account, today, zone, interest);
        }
        // 6.FETCH LEDGER AFTER INTEREST
        BigDecimal totalDebits = defaultZero(ledgerEntryRepository.sumDebitsForPeriod(accountId, start, end));
        BigDecimal totalCredits = defaultZero(ledgerEntryRepository.sumCreditsForPeriod(accountId, start, end));
        // 7. FINAL CALCULATION (Closing balance (ledger is 'source' of truth))
        BigDecimal closingBalance =
                openingBalance
                        .add(totalDebits)
                        .subtract(totalCredits);
        BigDecimal totalDue = closingBalance.max(BigDecimal.ZERO);
        BigDecimal minDue = calculateMinimumDue(account, closingBalance);
        LocalDate dueDate = today.plusDays(account.getGracePeriodDays());
        // 8. Build statement
        BillingStatement statement = buildStatement(
                account, startDate, today, openingBalance, totalDebits, totalCredits,
                interest, closingBalance, totalDue, minDue, dueDate,BigDecimal.ZERO, now
        );
        log.info("Saving statement | accountId={} | totalDue={} | minDue={}",
                accountId, totalDue, minDue);
        BillingStatement saved = billingRepository.save(statement);
        log.info("Statement saved | statementId={} | accountId={}",
                saved.getStatementId(), accountId);
        //  9. Update account snapshot (last statement balance, due date, minimum due)
        accountService.updateAccountAfterBilling(
                accountId,
                now.toInstant(),
                closingBalance,
                dueDate.atStartOfDay(zone).toInstant(),
                minDue
        );

        log.info("Statement generated successfully | accountId={}", accountId);
        return mapper.toResponse(saved);
    }
    
    /**
     * Posts interest as a system transaction.
     */
    private void postInterestTransaction(
            CreditAccount account,
            LocalDate today,
            ZoneId zone,
            BigDecimal interest) {

        String ref = "INT-" +
                account.getAccountId().toString().substring(0, 8) +
                "-" + today.toString().replace("-", "");

        Instant interestTime = today.atTime(23, 59).atZone(zone).toInstant();

        transactionService.postSystemTransaction(
                account,
                TransactionType.INTEREST,
                interest,
                "Interest Charged",
                ref,
                interestTime
        );

        entityManager.flush();
    }
    /**
     * Enforces the billing schedule based on the customer's localized timezone.
     * @param account Credit Account
     * @param today Today
     */
    private void validateBillingCycle(CreditAccount account, LocalDate today) {

        LocalDate expected = safeBillingDate(today, account.getStatementCycleDay());

        log.info("Billing cycle check | accountId={} | today={} | expectedCycleDay={}",
                account.getAccountId(), today, expected);

        if (!today.equals(expected)) {
            log.warn("Skipping account {}: billing cycle not reached (today={}, expected={})",
                    account.getAccountId(), today, expected);

            throw new BadRequestException("Billing cycle not reached yet");
        }
    }

    /**
     * Billing Date Resolve
     * @param baseDate
     * @param cycleDay
     * @return
     */
    private LocalDate resolveBillingDate(LocalDate baseDate, int cycleDay) {

        LocalDate cycleDateThisMonth = safeBillingDate(baseDate, cycleDay);

        if (baseDate.isBefore(cycleDateThisMonth)) {
            LocalDate previousMonth = baseDate.minusMonths(1);
            return safeBillingDate(previousMonth, cycleDay);
        }

        return cycleDateThisMonth;
    }

    /**
     * Duplicate Statement Generation Check(idempotency)
     * @param accountId Credit Account to which the statement will be generated
     * @param today Today's Date
     */
    private void preventDuplicate(UUID accountId, LocalDate today) {

        boolean exists = billingRepository
                .existsByAccountAccountIdAndBillingPeriodEnd(accountId, today);

        log.info("Duplicate check | accountId={} | date={} | exists={}",
                accountId, today, exists);

        if (exists) {
            log.warn("Skipping account {}: statement already exists for date {}", accountId, today);
            throw new BadRequestException("Statement already generated");
        }
    }

    /**
     * Determines the exact start date for the new billing cycle to ensure zero gaps in the ledger.
     * If the customer has a previous statement, the new period MUST start 
     * exactly 1 day after the last period ended. 
     * If this is their first ever statement, it falls back to the exact date the account was activated.
     * @param account Credit Account
     * @param lastStatement Last Billing statement
     * @param zone Timezone
     * @return Start date from last statement
     */
    private LocalDate resolveStartDate(CreditAccount account,
                                       Optional<BillingStatement> lastStatement,
                                       ZoneId zone) {
        return lastStatement
                .map(s -> s.getBillingPeriodEnd().plusDays(1))
                .orElse(account.getActivatedAt().atZone(zone).toLocalDate());
    }

    /**
     * Calculates the minimum payment required for a billing cycle.
     *
     * <p><b>Rules:</b></p>
     * <ul>
     *     <li>If balance ≤ 0 → minimum due = 0</li>
     *     <li>Minimum due = max (percentage of balance, product floor amount)</li>
     *     <li>Capped at total closing balance</li>
     * </ul>
     *
     * <p><b>Formula:</b><br>
     * min (max(closingBalance × percentage, floorAmount), closingBalance)</p>
     *
     * @param account credit account
     * @param closingBalance closing balance
     * @return minimum due amount
     */
    private BigDecimal calculateMinimumDue(CreditAccount account, BigDecimal closingBalance) {
        if (closingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentDue = closingBalance
                .multiply(account.getMinimumDuePercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return percentDue
                .max(account.getCreditProduct().getMinimumDueAmount())
                .min(closingBalance);
        
    }

    /**
     * Constructs a {@link BillingStatement} entity with all computed financial values
     * for a billing cycle.
     *
     * <p>This method acts as a factory for creating a fully populated billing statement
     * before persistence. It ensures all monetary fields and metadata are correctly
     * initialized.</p>
     *
     * <p><b>Responsibilities:</b></p>
     * <ul>
     *     <li>Assign billing period (start & end dates)</li>
     *     <li>Set financial values (opening, debits, credits, interest, closing)</li>
     *     <li>Initialize dues (total due, minimum due, remaining amount)</li>
     *     <li>Set payment tracking fields (amount paid = 0 initially)</li>
     *     <li>Assign statement metadata (status, generation timestamp)</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>{@code remainingAmount} is initialized equal to {@code totalDue}</li>
     *     <li>Statement status is set to {@link StatementStatus#GENERATED}</li>
     *     <li>All monetary values are expected to be pre-calculated before calling this method</li>
     * </ul>
     *
     * @param account     the credit account associated with the statement
     * @param startDate   start date of the billing period
     * @param endDate     end date of the billing period
     * @param opening     opening balance at the beginning of the cycle
     * @param debits      total debits (spends) during the cycle
     * @param credits     total credits (payments) during the cycle
     * @param interest    interest charged for the billing cycle
     * @param closing     closing balance after all calculations
     * @param totalDue    total amount due (non-negative)
     * @param minDue      minimum payment required
     * @param dueDate     payment due date
     * @param now         timestamp when the statement is generated
     *
     * @return fully constructed {@link BillingStatement} ready for persistence
     */
    private BillingStatement buildStatement(
            CreditAccount account,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal opening,
            BigDecimal debits,
            BigDecimal credits,
            BigDecimal interest,
            BigDecimal closing,
            BigDecimal totalDue,
            BigDecimal minDue,
            LocalDate dueDate,
            BigDecimal lateFee,
            ZonedDateTime now
    ) {
        BillingStatement statement = new BillingStatement();

        statement.setAccount(account);
        statement.setBillingPeriodStart(startDate);
        statement.setBillingPeriodEnd(endDate);
        statement.setOpeningBalance(opening);
        statement.setTotalDebits(debits);
        statement.setTotalCredits(credits);
        statement.setInterestCharged(interest);
        statement.setClosingBalance(closing);
        statement.setTotalAmountDue(totalDue);
        statement.setRemainingAmount(totalDue); 
        statement.setMinimumDueAmount(minDue);
        statement.setMinDuePercent(account.getMinimumDuePercent());
        statement.setMinDueFloor(account.getCreditProduct().getMinimumDueAmount());
        statement.setDueDate(dueDate);
        statement.setLateFee(
        	    lateFee != null ? lateFee : BigDecimal.ZERO
        	);
        statement.setAmountPaid(BigDecimal.ZERO);
        statement.setStatementStatus(StatementStatus.GENERATED);
        statement.setGeneratedAt(now.toInstant());

        return statement;
    }


    /**
     * Evaluates the payment status of a statement after its due date.
     *
     * <p>Determines status based on payments made before cutoff:</p>
     * <ul>
     *     <li>PAID → total amount fully paid</li>
     *     <li>REVOLVING → minimum due paid</li>
     *     <li>OVERDUE → less than minimum due paid</li>
     * </ul>
     *
     * <p>Triggers late fee application for overdue accounts.</p>
     *
     * @param statement billing statement to evaluate
     */
    private void evaluateDueDateOutcome(BillingStatement statement) {

        ZoneId zone = timezoneResolver.resolve(
                statement.getAccount().getCustomer()
        );

        Instant cutoff = statement.getDueDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant();

        BigDecimal paidByDueDate =
                paymentAllocationRepository.sumAllocatedToStatementBefore(
                        statement.getStatementId(),
                        cutoff
                );

        if (paidByDueDate.compareTo(statement.getTotalAmountDue()) >= 0) {
            statement.setStatementStatus(StatementStatus.PAID);
            return;
        }

        if (paidByDueDate.compareTo(statement.getMinimumDueAmount()) >= 0) {
            statement.setStatementStatus(StatementStatus.REVOLVING);
            return;
        }

        statement.setStatementStatus(StatementStatus.OVERDUE);

        applyLateFee(statement);
    }
    /**
     * Applies a late fee to an overdue statement.
     *
     * <p>Ensures idempotency — late fee is applied only once.</p>
     *
     * <p>Updates:</p>
     * <ul>
     *     <li>Remaining amount</li>
     *     <li>Total due</li>
     *     <li>Closing balance</li>
     * </ul>
     *
     * @param statement billing statement
     */
    private void applyLateFee(BillingStatement statement) {

        if (Boolean.TRUE.equals(statement.getLateFeeApplied())) {
            return;
        }

        ZoneId zone = timezoneResolver.resolve(
                statement.getAccount().getCustomer()
        );

        BigDecimal lateFee =
                statement.getAccount()
                        .getCreditProduct()
                        .getLateFeeAmount();

        Instant feeTime = statement.getDueDate()
                .atTime(23, 59)
                .atZone(zone)
                .toInstant();

        statement.setLateFee(lateFee);
        statement.setLateFeeApplied(true);
        statement.setLateFeeAppliedAt(feeTime);  

        statement.setRemainingAmount(
                statement.getRemainingAmount().add(lateFee)
        );

        statement.setTotalAmountDue(
                statement.getTotalAmountDue().add(lateFee)
        );

        statement.setClosingBalance(
                statement.getClosingBalance().add(lateFee)
        );

        String ref = "LATE-" + statement.getStatementId();

        transactionService.postSystemTransaction(
                statement.getAccount(),
                TransactionType.FEE,
                lateFee,
                "Late Fee",
                ref,
                feeTime  
        );
    }

    /**
     * Null-safe wrapper for financial calculations.
     * Prevents NullPointerExceptions caused by SQL aggregate functions (like SUM) 
     * returning 'null' when an account has no transactions for the period.
     * @param value Big decimal
     * @return Some Value other than Null
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Safely bounds the requested billing cycle day against the actual length of the current month.
     * Prevents the "February 30th" bug where the application crashes trying to construct an invalid date.
     * @param baseDate Current day
     * @param cycleDay Cycle Day
     * @return safe Billing date
     */
    private LocalDate safeBillingDate(LocalDate baseDate, int cycleDay) {
    	// Prevents the "February 30th" bug
        return baseDate.withDayOfMonth(Math.min(cycleDay, baseDate.lengthOfMonth()));
    }
    
    /**
     * Call By payment system 
     */
	@Override
	public List<BillingStatement> getUnpaidStatementsOldestFirst(UUID accountId) {
		return billingRepository.findUnpaidStatementsOldestFirst(accountId);
	}
	
	/**
	 * Late fee post if it is overdue
	 * @param lastStatement
	 * @param zone
	 * @return
	 */
	private boolean ensureLateFeeAppliedIfOverdue(BillingStatement lastStatement, ZoneId zone) {

	    if (lastStatement == null) return false;

	    if (Boolean.TRUE.equals(lastStatement.getLateFeeApplied())) return false;

	    LocalDate today = LocalDate.now(zone);

	    if (today.isAfter(lastStatement.getDueDate())
	            && lastStatement.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {

	        applyLateFee(lastStatement);
	        return true;
	    }

	    return false;
	}

}
