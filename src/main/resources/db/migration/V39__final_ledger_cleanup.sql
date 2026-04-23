-- =========================
-- 1. REMOVE LEGACY BALANCE COLUMNS
-- =========================

ALTER TABLE transactions
    DROP COLUMN IF EXISTS balance_before,
    DROP COLUMN IF EXISTS balance_after;

-- =========================
-- 2. REMOVE STATEMENT COUPLING
-- =========================

ALTER TABLE transactions
    DROP COLUMN IF EXISTS statement_id;

-- =========================
-- 3. ENFORCE LEDGER UNIQUENESS (IF NOT ALREADY)
-- =========================

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'unique_ledger_reference'
    ) THEN
        ALTER TABLE ledger_entries
        ADD CONSTRAINT unique_ledger_reference
        UNIQUE (reference_type, reference_id);
    END IF;
END$$;

-- =========================
-- 4. ADD STRICT NOT NULL (LEDGER SAFETY)
-- =========================

ALTER TABLE ledger_entries
    ALTER COLUMN reference_type SET NOT NULL,
    ALTER COLUMN reference_id SET NOT NULL;

-- =========================
-- 5. ADD INDEX FOR PERFORMANCE
-- =========================

CREATE INDEX IF NOT EXISTS idx_ledger_account_time
ON ledger_entries(account_id, created_at);

-- =========================
-- 6. CLEAN UNUSED INDEXES
-- =========================

DROP INDEX IF EXISTS idx_txn_type_status;
