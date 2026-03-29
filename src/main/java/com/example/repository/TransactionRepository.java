package com.example.repository;

import com.example.entity.Transaction;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Customer — all transactions on their account (newest first)
    List<Transaction> findAllByAccountAccountIdOrderByTransactionTimeDesc(UUID accountId);

    // DYNAMIC FILTER (CUSTOM QUERY)
    @Query("""
    	    SELECT t FROM Transaction t
    	    WHERE t.account.accountId = :accountId
    	    AND (COALESCE(:status, t.transactionStatus) = t.transactionStatus)
    	    AND (COALESCE(:type, t.transactionType) = t.transactionType)
    	    AND (COALESCE(:cardId, t.card.cardId) = t.card.cardId)
    	    ORDER BY t.transactionTime DESC
    	""")
    	List<Transaction> findByFilters(
    	        @Param("accountId") UUID accountId,
    	        @Param("status") TransactionStatus status,
    	        @Param("type") TransactionType type,
    	        @Param("cardId") UUID cardId
    	);
    // Global Filter(ADMIN)Custom Query
    @Query("""
    	    SELECT t FROM Transaction t
    	    WHERE (COALESCE(:status, t.transactionStatus) = t.transactionStatus)
    	    AND (COALESCE(:type, t.transactionType) = t.transactionType)
    	    AND (COALESCE(:accountId, t.account.accountId) = t.account.accountId)
    	    AND (COALESCE(:userId, t.account.customer.user.userId) = t.account.customer.user.userId)
    	    ORDER BY t.transactionTime DESC
    	""")
    	List<Transaction> findAllWithFilters(
    	        @Param("status") TransactionStatus status,
    	        @Param("type") TransactionType type,
    	        @Param("accountId") UUID accountId,
    	        @Param("userId") UUID userId
    	);
    // ── Daily limit check ──
    // Sum of APPROVED transaction amounts for a specific card,
    // of a given type, from a given time (start of today) onwards.
    // Used to enforce posDailyLimit (PURCHASE) and ecommerceDailyLimit (ONLINE).
    
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
}