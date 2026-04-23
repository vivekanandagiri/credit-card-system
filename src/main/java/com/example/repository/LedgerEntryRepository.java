package com.example.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entity.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        SELECT COALESCE(SUM(l.amount),0)
        FROM LedgerEntry l
        WHERE l.accountId = :accountId
        AND l.entryType = 'CREDIT'
    """)
    BigDecimal sumCredits(UUID accountId);

    @Query("""
        SELECT COALESCE(SUM(l.amount),0)
        FROM LedgerEntry l
        WHERE l.accountId = :accountId
        AND l.entryType = 'DEBIT'
    """)
    BigDecimal sumDebits(UUID accountId);
    
    @Query("""
    	    SELECT COALESCE(SUM(l.amount), 0)
    	    FROM LedgerEntry l
    	    WHERE l.accountId = :accountId
    	      AND l.entryType = 'DEBIT'
    	      AND l.createdAt >= :start
    	      AND l.createdAt < :end
    	""")
    	BigDecimal sumDebitsForPeriod(UUID accountId, Instant start, Instant end);

    	@Query("""
    	    SELECT COALESCE(SUM(l.amount), 0)
    	    FROM LedgerEntry l
    	    WHERE l.accountId = :accountId
    	      AND l.entryType = 'CREDIT'
    	      AND l.createdAt >= :start
    	      AND l.createdAt < :end
    	""")
    	BigDecimal sumCreditsForPeriod(UUID accountId, Instant start, Instant end);

	List<LedgerEntry> findByAccountIdAndCreatedAtBetween(UUID accountId, Instant start, Instant end);
}