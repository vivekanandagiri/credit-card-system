package com.example.repository;

import com.example.entity.CreditAccount;
import com.example.enums.AccountStatus;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, UUID> {

	// Customer's own accounts
	List<CreditAccount> findAllByCustomerCustomerId(UUID customerId);

	// Check if account exists for application (prevent duplicate account creation)
	boolean existsByApplicationApplicationId(UUID applicationId);

	// Check active account for a product — used by ActiveAccountCheker
	boolean existsByCustomerCustomerIdAndCreditProductCreditProductIdAndAccountStatus(UUID customerId,
			Long creditProductId, AccountStatus status);

	// find account by status
	List<CreditAccount> findAllByCustomerCustomerIdAndAccountStatus(UUID customerId, AccountStatus status);

	// Find by account number
	Optional<CreditAccount> findByAccountNumber(String accountNumber);

	// Admin — filter by status
	List<CreditAccount> findAllByAccountStatus(AccountStatus status);

	// Pessimistic Locking Method
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3 sec timeout
	})
	Optional<CreditAccount> findByAccountId(UUID accountId);
	
	//Scheduler Used: Fetch the account which have billing day today
	@Query("""
		    SELECT a FROM CreditAccount a
		    WHERE a.accountStatus = 'ACTIVE'
		    AND 
		        CASE 
		            WHEN a.statementCycleDay > :lastDay 
		            THEN :lastDay 
		            ELSE a.statementCycleDay 
		        END = :todayDay
		""")
		List<CreditAccount> findAccountsForBillingDay(
		        @Param("todayDay") int todayDay,
		        @Param("lastDay") int lastDay
		);
		
	//Billing Scheduler
	//FInd Active account accroding to user time zone 
	@Query("""
		    SELECT a FROM CreditAccount a
		    WHERE a.accountStatus = 'ACTIVE'
		""")
		List<CreditAccount> findAllActiveAccounts();
	
	// 🔒 CRITICAL: Lock account during payment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CreditAccount a WHERE a.accountId = :id")
    Optional<CreditAccount> findByIdForUpdate(@Param("id") UUID id);
    
    
}