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

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

// ✅ CUSTOMER QUERIES
	List<Transaction> findAllByAccountAccountIdOrderByTransactionTimeDesc(UUID accountId);

// ✅ DAILY LIMIT CHECK
	@Query("""
			SELECT COALESCE(SUM(t.amount), 0)
			FROM Transaction t
			WHERE t.card.cardId         = :cardId
			  AND t.transactionType     = :type
			  AND t.transactionChannel  = :channel
			  AND t.transactionStatus   = 'APPROVED'
			  AND t.transactionTime    >= :from
			""")
	BigDecimal sumApprovedAmountByCardAndTypeAndChannelAfter(@Param("cardId") UUID cardId,
			@Param("type") TransactionType type, @Param("channel") TransactionChannel channel,
			@Param("from") Instant from);

// ✅ INTEREST
	@Query("""
			SELECT t FROM Transaction t
			WHERE t.account.accountId = :accountId
			AND t.transactionTime >= :start
			AND t.transactionTime < :end
			AND t.transactionType = :type
			ORDER BY t.transactionTime ASC
			""")
	List<Transaction> findTransactionsForInterest(@Param("accountId") UUID accountId, @Param("start") Instant start,
			@Param("end") Instant end, @Param("type") TransactionType type);

// 🔥 FIXED METHODS (IMPORTANT)

// For API idempotency (BEST PRACTICE)
	Optional<Transaction> findByNetworkReference(String networkReference);

// For internal system reference (optional)
	Optional<Transaction> findByInternalReference(String internalReference);
}