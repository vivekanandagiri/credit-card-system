package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.LedgerEntry;
import com.example.enums.EntryType;
import com.example.repository.LedgerEntryRepository;
import com.example.service.LedgerService;

import lombok.RequiredArgsConstructor;
/**
 * Implementation of {@link LedgerService} responsible for maintaining
 * financial ledger entries for accounts.
 *
 * <p><b>Core Responsibilities:</b></p>
 * <ul>
 *     <li>Record debit and credit transactions</li>
 *     <li>Maintain immutable transaction history</li>
 *     <li>Provide balance calculations</li>
 * </ul>
 *
 * <p><b>Ledger Model:</b></p>
 * <ul>
 *     <li>DEBIT → increases outstanding balance (customer owes more)</li>
 *     <li>CREDIT → decreases outstanding balance (customer pays back)</li>
 *     <li>Balance = Total Credits - Total Debits</li>
 * </ul>
 *
 * <p><b>Design Notes:</b></p>
 * <ul>
 *     <li>Ledger is append-only (no updates, only inserts)</li>
 *     <li>Each entry is tied to a reference (transaction, payment, etc.)</li>
 *     <li>Used as the source of truth for account balance</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LedgerServiceImpl implements LedgerService{

	private final LedgerEntryRepository repo;

	/**
     * Records a debit entry (increases liability).
     *
     * @param accountId     account identifier
     * @param amount        amount to debit (must be > 0)
     * @param referenceType type of reference (e.g., PURCHASE, FEE)
     * @param referenceId   reference identifier
     * @param createdAt     timestamp (optional, defaults to now)
     */
    @Override
    public void debit(UUID accountId, BigDecimal amount,
            String referenceType, UUID referenceId,
            Instant createdAt){

        validateAmount(amount);

        repo.save(buildEntry(
                accountId,
                EntryType.DEBIT,
                amount,
                referenceType,
                referenceId,
                createdAt
        ));
    }

    /**
     * Records a credit entry (reduces liability).
     *
     * @param accountId     account identifier
     * @param amount        amount to credit (must be > 0)
     * @param referenceType type of reference (e.g., PAYMENT, REFUND)
     * @param referenceId   reference identifier
     * @param createdAt     timestamp (optional, defaults to now)
     */
    @Override
    public void credit(UUID accountId, BigDecimal amount,
                       String referenceType, UUID referenceId,Instant createdAt) {

        validateAmount(amount);

        repo.save(buildEntry(
                accountId,
                EntryType.CREDIT,
                amount,
                referenceType,
                referenceId,
                createdAt
        ));
    }

    /**
     * Calculates current balance for the account.
     *
     * <p>Formula:</p>
     * <pre>
     * Balance = Total Credits - Total Debits
     * </pre>
     *
     * @param accountId account identifier
     * @return current balance
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {

        BigDecimal credits = getTotalCredits(accountId);
        BigDecimal debits = getTotalDebits(accountId);

        return credits.subtract(debits);
    }

    /**
     * Returns total credited amount for the account.
     *
     * @param accountId account identifier
     * @return total credits (defaults to ZERO if none)
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalCredits(UUID accountId) {
        return repo.sumCredits(accountId) != null
                ? repo.sumCredits(accountId)
                : BigDecimal.ZERO;
    }
    /**
     * Returns total debited amount for the account.
     *
     * @param accountId account identifier
     * @return total debits (defaults to ZERO if none)
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalDebits(UUID accountId) {
        return repo.sumDebits(accountId) != null
                ? repo.sumDebits(accountId)
                : BigDecimal.ZERO;
    }

    // =============== INTERNAL HELPERS ===================

    /**
     * Builds a ledger entry entity.
     *
     * @param accountId     account ID
     * @param type          entry type (DEBIT/CREDIT)
     * @param amount        transaction amount
     * @param referenceType source/type of transaction
     * @param referenceId   reference ID
     * @param createdAt     timestamp (nullable)
     * @return {@link LedgerEntry}
     */
    private LedgerEntry buildEntry(UUID accountId,
                                  EntryType type,
                                  BigDecimal amount,
                                  String referenceType,
                                  UUID referenceId,
                                  Instant createdAt) {

        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .entryType(type)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdAt(createdAt != null ? createdAt : Instant.now())
                .build();
    }
    /**
     * Validates that the transaction amount is positive.
     *
     * @param amount transaction amount
     * @throws IllegalArgumentException if amount is null or ≤ 0
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    /**
     * Deletes ledger entries by reference ID.
     *
     * <p><b>Note:</b> This operation should be used cautiously as it
     * breaks the append-only nature of the ledger.</p>
     *
     * @param transactionId reference ID
     */
    @Override
	public void deleteByReferenceId(UUID transactionId) {
		// TODO Auto-generated method stub
		
	}
}