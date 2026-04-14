package com.example.repository;

import com.example.entity.Transaction;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>,JpaSpecificationExecutor<Transaction> {

    // 1. CUSTOMER QUERIES

    List<Transaction> findAllByAccountAccountIdOrderByTransactionTimeDesc(UUID accountId);

//    // Dynamic filter (Customer)
    /**
     * facing some Null issue so i am not using this 
     */
//    @Query("""
//    	    SELECT t
//    	    FROM Transaction t
//    	    WHERE t.account.accountId = :accountId
//    	      AND (:status IS NULL OR t.transactionStatus = :status)
//    	      AND (:type IS NULL OR t.transactionType = :type)
//    	      AND (:cardId IS NULL OR t.card.cardId = :cardId)
//    	""")
//    	Page<Transaction> findByFilters(
//    	        @Param("accountId") UUID accountId,
//    	        @Param("status") TransactionStatus status,
//    	        @Param("type") TransactionType type,
//    	        @Param("cardId") UUID cardId,
//    	        Pageable pageable
//    	);


    // 3. DAILY LIMIT CHECK

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.card.cardId         = :cardId
          AND t.transactionType     = :type
          AND t.transactionChannel  = :channel
          AND t.transactionStatus   = 'APPROVED'
          AND t.transactionTime    >= :from
    """)
    BigDecimal sumApprovedAmountByCardAndTypeAndChannelAfter(
            @Param("cardId") UUID cardId,
            @Param("type") TransactionType type,
            @Param("channel") TransactionChannel channel,
            @Param("from") Instant from
    );

    // 4. BILLING SUPPORT 

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN t.transactionType IN ('PURCHASE','FEE','INTEREST') THEN t.amount
                WHEN t.transactionType IN ('PAYMENT','REFUND') THEN -t.amount
            END
        ), 0)
        FROM Transaction t
        WHERE t.account.accountId = :accountId
          AND t.transactionStatus = 'APPROVED'
          AND t.transactionTime BETWEEN :start AND :end
    """)
    BigDecimal sumNetTransactionsForPeriod(
            @Param("accountId") UUID accountId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
    		SELECT COALESCE(SUM(t.amount), 0)
    		FROM Transaction t
    		WHERE t.account.accountId = :accountId
    		  AND t.transactionStatus = 'APPROVED'
    		  AND t.transactionType IN ('PURCHASE','FEE','INTEREST')
    		  AND t.transactionTime >= :start AND t.transactionTime < :end
    		""")
    	BigDecimal sumDebitsForPeriod(@Param("accountId") UUID accountId,
    	                               @Param("start") Instant start,
    	                               @Param("end") Instant end);

    @Query("""
    		SELECT COALESCE(SUM(t.amount), 0)
    		FROM Transaction t
    		WHERE t.account.accountId = :accountId
    		  AND t.transactionStatus = 'APPROVED'
    		  AND t.transactionType IN ('PAYMENT','REFUND')
    		  AND t.transactionTime >= :start AND t.transactionTime < :end
    		""")
    	BigDecimal sumCreditsForPeriod(@Param("accountId") UUID accountId,
    	                                @Param("start") Instant start,
    	                                @Param("end") Instant end);

    
    //intrest
    @Query("""
    	    SELECT t FROM Transaction t
    	    WHERE t.account.accountId = :accountId
    	    AND t.transactionTime >= :start
    	    AND t.transactionTime < :end
    	    AND t.transactionType = :type
    	    ORDER BY t.transactionTime ASC
    	""")
    	List<Transaction> findTransactionsForInterest(
    	        @Param("accountId") UUID accountId,
    	        @Param("start") Instant start,
    	        @Param("end") Instant end,
    	        @Param("type") TransactionType type
    	);

	Optional<Transaction> findByTransactionReference(String transactionReference);

	Optional<Transaction> findByReferenceNumber(String reference);
}