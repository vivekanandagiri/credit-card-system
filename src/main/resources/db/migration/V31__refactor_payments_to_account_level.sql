-- Enable UUID generation (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- STEP 1: Add account_id to payments
ALTER TABLE payments
ADD COLUMN account_id UUID;

-- STEP 2: Backfill account_id from statement_id
UPDATE payments p
SET account_id = bs.account_id
FROM billing_statements bs
WHERE p.statement_id = bs.statement_id;

-- STEP 3: Make account_id mandatory
ALTER TABLE payments
ALTER COLUMN account_id SET NOT NULL;

-- STEP 4: Add FK to credit_accounts
ALTER TABLE payments
ADD CONSTRAINT fk_payments_account
FOREIGN KEY (account_id)
REFERENCES credit_accounts(account_id);

-- STEP 5: Create payment_allocations table
CREATE TABLE payment_allocations (
    allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    statement_id UUID NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,

    CONSTRAINT fk_payment_allocations_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_payment_allocations_statement
        FOREIGN KEY (statement_id)
        REFERENCES billing_statements(statement_id)
);

-- STEP 6: Backfill allocations from legacy payments
-- Historical payments were 1:1 with statements
INSERT INTO payment_allocations (
    payment_id,
    statement_id,
    allocated_amount
)
SELECT
    p.payment_id,
    p.statement_id,
    p.amount
FROM payments p
WHERE p.statement_id IS NOT NULL;

-- STEP 7: Drop old statement FK from payments
ALTER TABLE payments
DROP CONSTRAINT IF EXISTS fk_payments_statement;

-- STEP 8: Remove legacy statement_id column
ALTER TABLE payments
DROP COLUMN statement_id;

-- STEP 9: Add Performance Indexes
CREATE INDEX idx_payments_account_id
    ON payments(account_id);

CREATE INDEX idx_payment_allocations_payment_id
    ON payment_allocations(payment_id);

CREATE INDEX idx_payment_allocations_statement_id
    ON payment_allocations(statement_id);