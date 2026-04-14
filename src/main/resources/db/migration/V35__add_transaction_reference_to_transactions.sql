--reference_number	--------------> Internal generated transaction/ledger ref
--transaction_reference----------->	External/client idempotency key for purchases
--payment_reference_id------------>	External payment idempotency key for bill payments
ALTER TABLE transactions
ADD COLUMN transaction_reference VARCHAR(100);

UPDATE transactions
SET transaction_reference = reference_number
WHERE transaction_type = 'PURCHASE'
  AND transaction_reference IS NULL;

ALTER TABLE transactions
ADD CONSTRAINT uk_transactions_transaction_reference
UNIQUE (transaction_reference);