

-- 1. Drop payments table (safe since no data)
DROP TABLE IF EXISTS payments;

-- 2. Drop unused enum (optional)

DROP TYPE IF EXISTS payment_method_enum;

-- 3. Ensure transaction_type_enum is correct

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum WHERE enumlabel = 'PAYMENT'
    ) THEN
        ALTER TYPE transaction_type_enum ADD VALUE 'PAYMENT';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_enum WHERE enumlabel = 'REFUND'
    ) THEN
        ALTER TYPE transaction_type_enum ADD VALUE 'REFUND';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_enum WHERE enumlabel = 'FEE'
    ) THEN
        ALTER TYPE transaction_type_enum ADD VALUE 'FEE';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_enum WHERE enumlabel = 'INTEREST'
    ) THEN
        ALTER TYPE transaction_type_enum ADD VALUE 'INTEREST';
    END IF;
END $$;

-- ============================================
-- 4. Ensure transactions table structure
-- ============================================

ALTER TABLE transactions
DROP COLUMN IF EXISTS payment_method;

ALTER TABLE transactions
ADD COLUMN IF NOT EXISTS statement_id UUID,
ADD COLUMN IF NOT EXISTS merchant_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS merchant_category_code VARCHAR(4),
ADD COLUMN IF NOT EXISTS merchant_category_name VARCHAR(100);

-- 5. Add FK constraint

ALTER TABLE transactions
DROP CONSTRAINT IF EXISTS fk_transactions_statement;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_statement
FOREIGN KEY (statement_id)
REFERENCES billing_statements(statement_id);

-- 6. Indexes (performance)

CREATE INDEX IF NOT EXISTS idx_txn_account 
ON transactions(credit_account_id);

CREATE INDEX IF NOT EXISTS idx_txn_time 
ON transactions(transaction_time);

CREATE INDEX IF NOT EXISTS idx_txn_type 
ON transactions(transaction_type);

CREATE INDEX IF NOT EXISTS idx_txn_status 
ON transactions(transaction_status);

