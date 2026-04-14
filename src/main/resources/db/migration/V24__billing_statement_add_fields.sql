-- 1. ADD NEW COLUMNS

ALTER TABLE billing_statements
ADD COLUMN total_debits NUMERIC(19,4) NOT NULL DEFAULT 0,
ADD COLUMN total_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
ADD COLUMN interest_charged NUMERIC(19,4) NOT NULL DEFAULT 0,
ADD COLUMN remaining_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
ADD COLUMN late_fee NUMERIC(19,4);

-- 2. MIGRATE EXISTING DATA

-- If total_transactions previously stored net (debits - credits),
-- we move it into total_debits (temporary assumption)
UPDATE billing_statements
SET total_debits = total_transactions,
    total_credits = 0;

-- Set remaining_amount = total_due - amount_paid
UPDATE billing_statements
SET remaining_amount = total_amount_due - amount_paid;

-- 3. DROP OLD COLUMN

ALTER TABLE billing_statements
DROP COLUMN total_transactions;

-- 4. ADD CONSTRAINT (IMPORTANT)

ALTER TABLE billing_statements
ADD CONSTRAINT uk_account_billing_period
UNIQUE (account_id, billing_period_end);

-- 5. ADD INDEX (PERFORMANCE)

CREATE INDEX idx_billing_account_id
ON billing_statements(account_id);