package com.example.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface LedgerService {

    // 🔴 Debit (money spent / liability increase)
	public void debit(UUID accountId, BigDecimal amount,
            String referenceType, UUID referenceId,
            Instant createdAt);

    // 🟢 Credit (payment / refund)
    void credit(UUID accountId, BigDecimal amount,
                String referenceType, UUID referenceId,Instant createdAt);

    // 📊 Current balance (credits - debits)
    BigDecimal getBalance(UUID accountId);

    // 📊 Optional helpers (useful for analytics / validation)
    BigDecimal getTotalCredits(UUID accountId);

    BigDecimal getTotalDebits(UUID accountId);

	/**
	 * Deletes ledger entries by reference ID.
	 *
	 * <p><b>Note:</b> This operation should be used cautiously as it
	 * breaks the append-only nature of the ledger.</p>
	 *
	 * @param transactionId reference ID
	 */
	void deleteByReferenceId(UUID transactionId);
}
