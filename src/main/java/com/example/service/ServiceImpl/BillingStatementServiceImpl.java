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
import com.example.repository.PaymentAllocationRepository;
import com.example.repository.TransactionRepository;
import com.example.service.*;

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
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *     <li>Generate billing statements (automated & manual)</li>
 *     <li>Ensure idempotent and timezone-aware billing execution</li>
 *     <li>Calculate balances, interest, and minimum dues</li>
 *     <li>Track payment outcomes (PAID, REVOLVING, OVERDUE)</li>
 *     <li>Apply penalties such as late fees</li>
 * </ul>
 *
 * <p><b>Financial Safety Guarantees:</b></p>
 * <ul>
 *     <li>All monetary calculations use {@link java.math.BigDecimal}</li>
 *     <li>Operations are idempotent to support retry-safe batch jobs</li>
 *     <li>Timezone-aware to prevent incorrect billing execution</li>
 * </ul>
 *
 * <p><b>Important:</b> This is a critical financial component. Any modification
 * must preserve ledger consistency and audit correctness.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BillingStatementServiceImpl implements BillingStatementService {

    private final BillingStatementRepository billingRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final CreditAccountService accountService;
    private final BillingStatementMapper mapper;
    private final TimezoneResolver timezoneResolver;
    private final InterestCalculationService interestService;
    private final PaymentAllocationRepository paymentAllocationRepository;

    /**
     * Generates a billing statement for the given account.
     *
     * <p>This method executes the full billing cycle pipeline:</p>
     * <ol>
     *     <li>Validate billing cycle date</li>
     *     <li>Ensure idempotency (prevent duplicates)</li>
     *     <li>Determine billing period</li>
     *     <li>Aggregate transactions (debits & credits)</li>
     *     <li>Calculate interest</li>
     *     <li>Compute balances and dues</li>
     *     <li>Persist statement and update account</li>
     * </ol>
     *
     * <p><b>Idempotency:</b> Ensures safe re-execution during batch retries.</p>
     *
     * @param accountId the credit account ID
     * @return generated billing statement response
     * @throws BadRequestException if billing cycle not reached or duplicate detected
     */
    @Override
    @Transactional // Override for write operation
    public BillingStatementResponse generateStatement(UUID accountId) {
        CreditAccount account = accountService.getAccountEntity(accountId);
        ZoneId zone = timezoneResolver.resolve(account.getCustomer());

        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        log.debug("Initiating statement generation | accountId={} | date={}", accountId, today);

        validateBillingCycle(account, today);
        preventDuplicate(accountId, today);

        //Fetch latest previous statement (if exists)
        Optional<BillingStatement> lastStatementOpt = billingRepository
                .findTopByAccountOrderByBillingPeriodEndDesc(account);

        LocalDate startDate = resolveStartDate(account, lastStatementOpt, zone);
        LocalDate endDate = today;

        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal totalDebits = defaultZero(transactionRepository.sumDebitsForPeriod(accountId, start, end));
        BigDecimal totalCredits = defaultZero(transactionRepository.sumCreditsForPeriod(accountId, start, end));

        BigDecimal openingBalance = defaultZero(account.getLastStatementBalance());

        BigDecimal interest = interestService.calculateInterest(
                accountId, start, end, account, lastStatementOpt.orElse(null), zone
        );

        if (interest.compareTo(BigDecimal.ZERO) > 0) {

            String ref = "INT-" + account.getAccountId() + "-" + endDate;

            transactionService.postSystemTransaction(
                    account,
                    TransactionType.INTEREST,
                    interest,
                    "Interest Charged",
                    ref
            );
        }
        BigDecimal closingBalance = calculateClosingBalance(openingBalance, totalDebits, totalCredits, interest);
        BigDecimal totalDue = closingBalance.max(BigDecimal.ZERO);
        BigDecimal minDue = calculateMinimumDue(account, closingBalance);

        LocalDate dueDate = endDate.plusDays(account.getGracePeriodDays());

        BillingStatement statement = buildStatement(
                account, startDate, endDate, openingBalance, totalDebits, totalCredits,
                interest, closingBalance, totalDue, minDue, dueDate, now
        );

        log.info("Saving statement | accountId={} | totalDue={} | minDue={}",
                accountId, totalDue, minDue);
        
        BillingStatement saved = billingRepository.save(statement);

        log.info("Statement saved | statementId={} | accountId={}",
                saved.getStatementId(), accountId);
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
     * Generates a billing statement bypassing billing cycle validation.
     *
     * <p>Used by administrators for manual interventions such as:
     * <ul>
     *     <li>Customer dispute resolution</li>
     *     <li>System correction</li>
     *     <li>Testing scenarios</li>
     * </ul>
     *
     * <p><b>Note:</b> Still enforces duplicate prevention.</p>
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
        LocalDate today = now.toLocalDate();

        log.info("Manual statement generation | accountId={} | date={}", accountId, today);

        // Skip billing cycle validation

        // Still prevent duplicates
        boolean exists = billingRepository
                .existsByAccountAccountIdAndBillingPeriodEnd(accountId, today);

        if (exists) {
            throw new BadRequestException("Statement already generated for today");
        }

        Optional<BillingStatement> lastStatementOpt =
                billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(account);

        LocalDate startDate = lastStatementOpt
                .map(s -> s.getBillingPeriodEnd().plusDays(1))
                .orElse(account.getActivatedAt().atZone(zone).toLocalDate());

        LocalDate endDate = today;

        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal totalDebits = defaultZero(
                transactionRepository.sumDebitsForPeriod(accountId, start, end));

        BigDecimal totalCredits = defaultZero(
                transactionRepository.sumCreditsForPeriod(accountId, start, end));

        BigDecimal openingBalance = defaultZero(account.getLastStatementBalance());

        BigDecimal interest = interestService.calculateInterest(
                accountId, start, end, account, lastStatementOpt.orElse(null), zone
        );

        BigDecimal closingBalance =
                openingBalance.add(totalDebits).subtract(totalCredits).add(interest);

        BigDecimal totalDue = closingBalance.max(BigDecimal.ZERO);

        BigDecimal minDue = calculateMinimumDue(account, closingBalance);

        LocalDate dueDate = endDate.plusDays(account.getGracePeriodDays());

        BillingStatement statement = buildStatement(
                account,
                startDate,
                endDate,
                openingBalance,
                totalDebits,
                totalCredits,
                interest,
                closingBalance,
                totalDue,
                minDue,
                dueDate,
                now
        );

        BillingStatement saved = billingRepository.save(statement);

        // Update account
        accountService.updateAccountAfterBilling(
                accountId,
                now.toInstant(),
                closingBalance,
                dueDate.atStartOfDay(zone).toInstant(),
                minDue
        );

        log.info("Manual statement generated | accountId={} | statementId={}",
                accountId, saved.getStatementId());

        return mapper.toResponse(saved);
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

        for (BillingStatement statement : dueStatements) {
            evaluateDueDateOutcome(statement);
        }

        billingRepository.saveAll(dueStatements);
    }
    /**
     * * Mark Due Statement for Reminder(e.g 7 days before due date )
     *  @param dueReminderDays Reminder customer before the dues dates
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
     * Get Particular Billing statement by statementId
     */
    @Override
    public BillingStatement getStatement(UUID statementId) {
        return billingRepository.findById(statementId)
                .orElseThrow(() -> new BadRequestException("Statement not found"));
    }
    
    /**
     * 
     */
    @Override
    public BillingStatement getStatementForUpdate(UUID statementId) {
        return billingRepository.findByIdForUpdate(statementId)
                .orElseThrow(() -> new BadRequestException("Statement not found"));
    }
    
    
    /**
     * Updates an existing statement, Save called by payment service.
     */
    @Override
    public BillingStatement save(BillingStatement statement) {
        return billingRepository.save(statement);
    }

    /**
     * Get All the Statements for a Particular account of a customer
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
     * Retrieves all billing statements for a specific account with strict ownership validation.
     * <p>
     * Note:The customer must prove the provided userId owns the requested accountId.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingStatementResponse> getCustomerStatementsByAccount(
            UUID userId,
            UUID accountId) {

        // Fetch account
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
    
    
    //=============================================Helper Methods ====================================================
    
    /**
     * Enforces the billing schedule based on the customer's localized timezone.
     * <p>
     * Architecture Note: Throwing an exception here during a batch job acts as a safe 
     * "skip" mechanism, ensuring we don't accidentally generate a statement days early 
     * due to a UTC vs. Local Timezone offset mismatch.
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
     * Financial Compliance: If the customer has a previous statement, the new period MUST start 
     * exactly 1 day after the last period ended. 
     * If this is their first ever statement,it falls back to the exact date the account was activated.
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
     * Closing Balance Calculation
     * @param opening Opening balance
     * @param debits total debits transactions
     * @param credits total credit transactions
     * @param interest total Interest
     * @return Closing balance
     */
    private BigDecimal calculateClosingBalance(BigDecimal opening,
                                               BigDecimal debits,
                                               BigDecimal credits,
                                               BigDecimal interest) {
        return opening.add(debits).subtract(credits).add(interest);
    }

    /**
     * Calculates the minimum payment required for a billing cycle.
     *
     * <p><b>Rules:</b></p>
     * <ul>
     *     <li>If balance ≤ 0 → minimum due = 0</li>
     *     <li>Minimum due = max(percentage of balance, product floor amount)</li>
     *     <li>Capped at total closing balance</li>
     * </ul>
     *
     * <p><b>Formula:</b><br>
     * min(max(closingBalance × percentage, floorAmount), closingBalance)</p>
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

        BigDecimal lateFee = 
        		statement.getAccount()
        		.getCreditProduct()
        		.getLateFeeAmount();

        statement.setLateFee(lateFee);
        statement.setLateFeeApplied(true);
        statement.setLateFeeAppliedAt(Instant.now());

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
                ref
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
	@Override
	public List<BillingStatement> getUnpaidStatementsOldestFirst(UUID accountId) {
		return billingRepository.findUnpaidStatementsOldestFirst(accountId);
	}


}
