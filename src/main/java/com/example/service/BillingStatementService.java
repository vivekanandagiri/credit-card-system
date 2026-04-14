package com.example.service;

import com.example.dto.response.BillingStatementResponse;
import com.example.entity.BillingStatement;

import java.util.List;
import java.util.UUID;

public interface BillingStatementService {

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
	 */
	BillingStatementResponse generateStatement(UUID accountId);

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
	BillingStatementResponse generateStatementManually(UUID accountId);

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
	void processDueStatements();

	/**
	 * * Mark Due Statement for Reminder(e.g 7 days before due date )
	 *  @param dueReminderDays Reminder customer before the dues dates
	 */
	void markDueStatements(int dueReminderDays);

	/**
	 * Get Particular Billing statement by statementId
	 */
	BillingStatement getStatement(UUID statementId);

	/**
	 *  Used in the Bill Payment
	 * @param statementId Statement Id
	 */
	BillingStatement getStatementForUpdate(UUID statementId);

	/**
	 * Updates an existing statement, Save called by payment service.
	 */
	BillingStatement save(BillingStatement statement);

	/**
	 * Get All the Statements for a Particular account of a customer
	 */
	List<BillingStatementResponse> getStatements(UUID accountId);

	/**
	 * Retrieves all billing statements for a specific account with strict ownership validation.
	 * <p>
	 * Note:The customer must prove the provided userId owns the requested accountId.
	 */
	List<BillingStatementResponse> getCustomerStatementsByAccount(UUID userId, UUID accountId);

	List<BillingStatement> getUnpaidStatementsOldestFirst(UUID accountId);

    
}