-- ============================================
-- V38: Ledger Enforcement + Cleanup (SAFE)
-- ============================================

-- =========================
-- 1. REMOVE OLD BILLING INDEX DEPENDENCIES
-- =========================

-- (optional safety: drop if exists)
DROP INDEX IF EXISTS idx_txn_type_status;

-- =========================
-- 2. ADD LEDGER SAFETY (CRITICAL)
-- =========================

-- Prevent duplicate ledger entries
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
-- 3. ADD NOT NULL SAFETY
-- =========================

-- Ensure critical fields are always present
ALTER TABLE ledger_entries
    ALTER COLUMN reference_type SET NOT NULL,
    ALTER COLUMN reference_id SET NOT NULL;

-- =========================
-- 4. PREPARE TRANSACTION TABLE (SOFT CLEANUP)
-- =========================

-- ⚠️ DO NOT DROP YET (just relax constraints)

ALTER TABLE transactions
    ALTER COLUMN balance_before DROP NOT NULL,
    ALTER COLUMN balance_after DROP NOT NULL;

-- =========================
-- 5. ADD AUTHORIZATION INDEX (PERFORMANCE)
-- =========================

CREATE INDEX IF NOT EXISTS idx_auth_network_ref
ON authorizations(network_reference);

-- =========================
-- 6. ADD LEDGER TIME INDEX (IMPORTANT FOR BILLING)
-- =========================

CREATE INDEX IF NOT EXISTS idx_ledger_account_time
ON ledger_entries(account_id, created_at);

-- =========================
-- 7. OPTIONAL: TRANSACTION CLEANUP PREP
-- =========================

-- Make statement_id nullable (decoupling billing)
ALTER TABLE transactions
    ALTER COLUMN statement_id DROP NOT NULL;

-- =========================
-- 8. DATA VALIDATION CHECK (SAFE GUARD)
-- =========================

-- Ensure no negative amounts (already exists but double safety)
ALTER TABLE ledger_entries
    ADD CONSTRAINT check_positive_amount
    CHECK (amount > 0);
