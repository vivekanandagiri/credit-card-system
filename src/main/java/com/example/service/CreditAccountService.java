package com.example.service;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCardApplication;
import com.example.enums.AccountStatus;
import com.example.enums.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CreditAccountService {

	// Called internally after application approval — not exposed as API
	CreditAccountResponse createAccount(CreditCardApplication application);

	// Get Accounts
    List<CreditAccountResponse> getAccounts(UUID userId, UserRole userRole, AccountStatus status);

	// Customer end points
    CreditAccountResponse getAccountById(UUID userId, UserRole role, UUID accountId);

	// Admin endpoints
	// ApiResponse<List<CreditAccountResponse>> getAllAccounts();
	// ApiResponse<List<CreditAccountResponse>> getAccountsByStatus(String status);
    CreditAccountResponse updateAccountStatus(UUID accountId,
            CreditAccountStatusUpdateRequest request);

	/**
	 * Returns the raw {@link CreditAccount} entity for internal service-to-service
	 * use.
	 *
	 * @throws ResourceNotFoundException if no account with this ID exists
	 */
	CreditAccount getAccountEntity(UUID accountId);

	/**
	 * Returns {@code true} if an account already exists for the given application.
	 * Used by the application service to guard against duplicate account creation.
	 */
	boolean accountExistsForApplication(UUID applicationId);

	/**
	 * Returns {@code true} if the customer holds an ACTIVE account for the given
	 * credit product. Used by {@link ActiveAccountChecker} implementations to
	 * enforce the duplicate-account gate without direct repository access.
	 */
	boolean hasActiveAccountForProduct(UUID customerId, Long creditProductId);

	/**
	 * Deducts {@code amount} from the account's available balance and adds it to
	 * the current (outstanding) balance. Persists the updated account.
	 *
	 * <p>
	 * Called by {@link TransactionService} so that the transaction domain does not
	 * need direct write access to the accounts repository.
	 *
	 * @throws BusinessRuleException if the available balance is insufficient
	 */
	void deductBalance(UUID accountId, BigDecimal amount);

	void addBalance(UUID accountId, BigDecimal amount);
	void updateAccountAfterBilling(
	        UUID accountId,
	        Instant lastStatementDate,
	        BigDecimal lastStatementBalance,
	        Instant nextDueDate,
	        BigDecimal minimumDueAmount
	);
	// Method for Payment Service 
	void applyPayment(UUID accountId, BigDecimal amount,Instant paidAt);

	CreditAccount getAccount(UUID accountId);

	CreditAccount getAccountForUpdate(UUID accountId);
}