-- 1. Add SYSTEM to transaction_channel enum
ALTER TYPE transaction_channel_enum ADD VALUE IF NOT EXISTS 'SYSTEM';


-- 2. Add UNIQUE constraint for user idempotency
-- (only for non-null values)
CREATE UNIQUE INDEX IF NOT EXISTS uk_transaction_reference
ON transactions(transaction_reference)
WHERE transaction_reference IS NOT NULL;