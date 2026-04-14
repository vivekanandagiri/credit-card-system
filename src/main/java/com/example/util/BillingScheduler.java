package com.example.util;

import com.example.entity.CreditAccount;
import com.example.exception.BadRequestException;
import com.example.repository.CreditAccountRepository;
import com.example.service.BillingStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Nightly billing scheduler.
 *
 * Runs two jobs at midnight every day:
 * 
 * Job 1 -- Statement Generation (00:00)
 *   Finds all ACTIVE accounts whose billing day is today and  service checks if statement should be generated 
 *   based on customer timezone and cycle day.
 *   Generates a billing statement for each such account.
 *   Example: If today is the 5th, all accounts with statement_cycle_day = 5 get a statement.
 *
 * Job 2 -- Due Marking (00:10)
 * 	 Find all Generated statement which are not paid 
 * 	 This is triggered for payment reminder 
 * 	 e.g: Before 7 days of the due date this scheduler will run for giving reminder
 * 
 * JOB 3 — DUE DATE EVALUATION
 * Runs daily after reminder job.
 *
 * Evaluates all statements whose due date has arrived/passed and
 * determines final outcome:
 *
 * PAID       -> full amount paid before cutoff
 * REVOLVING  -> minimum due paid before cutoff
 * OVERDUE    -> minimum due not paid before cutoff
 *
 * Applies late fee when overdue.
 *
 *
 * To enable scheduling, add @EnableScheduling to your main application class:
 *   @SpringBootApplication
 *   @EnableScheduling
 *   public class CreditCardSystemApplication { ... }
 *
 */
@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    @Value("${billing.due-reminder-days}")
    private int dueReminderDays;

    private final BillingStatementService billingStatementService;
    private final CreditAccountRepository accountRepository;

    public BillingScheduler(BillingStatementService billingStatementService,
                             CreditAccountRepository accountRepository) {
        this.billingStatementService = billingStatementService;
        this.accountRepository       = accountRepository;
    }

    /**
     * JOB 1 — STATEMENT GENERATION
     * Runs every day at 00:00 (midnight)
     * Cron Format: second | minute | hour | day | month | weekday
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void generateDailyStatements() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        log.info("Billing scheduler started for date {}", today);

        int todayDay = today.getDayOfMonth();
        int lastDay  = today.lengthOfMonth();

        List<CreditAccount> accounts =
                accountRepository.findAccountsForBillingDay(todayDay, lastDay);

        if (accounts.isEmpty()) {
            log.info("No accounts due for billing today");
            return;
        }

        log.info("Found {} account(s) due for statement generation", accounts.size());

        int successCount = 0;
        int failCount    = 0;
        int skipCount    = 0;

        for (CreditAccount account : accounts) {
            try {
                billingStatementService.generateStatement(account.getAccountId());
                successCount++;
            }catch (BadRequestException e) {
            	skipCount++;
            	log.debug("Skipped account {}: {}", account.getAccountId(), e.getMessage());
			}
            
            catch (Exception e) {
                failCount++;
                log.error("Failed for accountId={} accountNumber={} error={}",
                        account.getAccountId(),
                        account.getAccountNumber(),
                        e.getMessage(), e);
                // Continue processing other accounts even if one fails
            }
        }

        log.info("Statement generation summary — success: {}, skipped: {}, failed: {}",
                successCount, skipCount, failCount);
    }
    
//    /**
//     * JOB 2 — DUE MARKING
//     * Runs every day at 00:10 (10 minutes after statement generation)
//     */
//    @Scheduled(cron = "0 10 0 * * *", zone = "UTC")
//    public void markDueStatements() {
//
//        log.info("Due statement scheduler started");
//
//        try {
//            billingStatementService.markDueStatements(dueReminderDays);
//        } catch (Exception e) {
//            log.error("Due marking job failed: {}", e.getMessage(), e);
//        }
//
//        log.info("Due statement scheduler completed");
//    }
    /**
     * JOB 3 — OVERDUE MARKING
     * Runs every day at 00:15 (15 minutes after statement generation)
     */
    @Scheduled(cron = "0 15 0 * * *", zone = "UTC")
    public void processDueStatements() {

        log.info("Due-date evaluation scheduler started");

        try {
            billingStatementService.processDueStatements();
        } catch (Exception e) {
            log.error("Due-date evaluation job failed: {}", e.getMessage(), e);
        }

        log.info("Due-date evaluation scheduler completed");
    }
}
