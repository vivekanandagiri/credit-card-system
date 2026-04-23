package com.example.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.enums.StatementStatus;

import jakarta.persistence.LockModeType;



public interface BillingStatementRepository extends JpaRepository<BillingStatement, UUID> {
	

	List<BillingStatement> findByAccountAccountIdOrderByBillingPeriodEndDesc(UUID accountId);


	// Scheduler — check if statement already generated for this account + period
	boolean existsByAccountAccountIdAndBillingPeriodEnd(UUID accountId, LocalDate periodEnd);
	/**
     * Statements overdue because minimum due not paid by due date.
     * Scheduler — find all GENERATED statements whose due date has passed
     * Used by scheduler to mark -> OVERDUE
     * Used nightly to mark overdue statements
     */
	@Query("""
		    SELECT s
		    FROM BillingStatement s
		    WHERE s.dueDate < :today
		      AND s.amountPaid < s.minimumDueAmount
		      AND s.statementStatus IN (
		          com.example.enums.StatementStatus.GENERATED,
		          com.example.enums.StatementStatus.REVOLVING
		      )
		""")
	List<BillingStatement> findOverdueStatements(@Param("today") LocalDate today);

	
	Optional<BillingStatement> findTopByAccountOrderByBillingPeriodEndDesc(CreditAccount account);

   
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BillingStatement s WHERE s.statementId = :id")
    Optional<BillingStatement> findByIdForUpdate(@Param("id") UUID id);

	/**
     * Statements approaching due date.
     * Used by scheduler to mark GENERATED -> DUE
     */
    @Query("""
        SELECT s
        FROM BillingStatement s
        WHERE s.remainingAmount > 0
          AND s.statementStatus = com.example.enums.StatementStatus.GENERATED
          AND s.dueDate BETWEEN :today AND :threshold
    """)
    //Due date Reminder 
    List<BillingStatement> findStatementsDueSoon(@Param("today")
    LocalDate today,@Param("threshold") LocalDate threshold);
    
    /*
     * Payment allocation
     * 
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM BillingStatement s
        WHERE s.account.accountId = :accountId
          AND s.remainingAmount > 0
        ORDER BY s.billingPeriodEnd ASC
    """)
    List<BillingStatement> findUnpaidStatementsOldestFirst(UUID accountId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    	    SELECT bs
    	    FROM BillingStatement bs
    	    WHERE bs.dueDate <= :today
    	      AND bs.statementStatus = com.example.enums.StatementStatus.GENERATED
    	""")
    	List<BillingStatement> findDueStatementsPendingEvaluation(
    	        @Param("today") LocalDate today
    	);
    
    List<BillingStatement> findByDueDateAndStatementStatus(
            LocalDate dueDate,
            StatementStatus status
    );
}

