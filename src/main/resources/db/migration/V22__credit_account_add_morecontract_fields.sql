-- ============================================
-- STEP 1: ADD NEW COLUMNS (NULLABLE)
-- ============================================

ALTER TABLE credit_accounts
ADD COLUMN IF NOT EXISTS grace_period_days INT,
ADD COLUMN IF NOT EXISTS minimum_due_percent DECIMAL(5,2),
ADD COLUMN IF NOT EXISTS late_fee_amount DECIMAL(19,4),
ADD COLUMN IF NOT EXISTS apr DECIMAL(5,2);

-- ============================================
-- STEP 2: BACKFILL DATA FROM CREDIT_PRODUCTS
-- ============================================

UPDATE credit_accounts ca
SET
    grace_period_days = cp.grace_period_days,
    minimum_due_percent = cp.minimum_due_percent,
    late_fee_amount = cp.late_fee_amount,
    apr = cp.apr_purchase
FROM credit_products cp
WHERE ca.credit_product_id = cp.credit_product_id
  AND (
        ca.grace_period_days IS NULL OR
        ca.minimum_due_percent IS NULL OR
        ca.late_fee_amount IS NULL OR
        ca.apr IS NULL
      );

-- ============================================
-- STEP 3: ADD SAFETY CHECKS (OPTIONAL)
-- ============================================

-- Ensure no nulls remain before enforcing constraints
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM credit_accounts
        WHERE grace_period_days IS NULL
           OR minimum_due_percent IS NULL
           OR late_fee_amount IS NULL
           OR apr IS NULL
    ) THEN
        RAISE EXCEPTION 'Backfill failed: NULL values still exist in credit_accounts';
    END IF;
END $$;

-- ============================================
-- STEP 4: SET NOT NULL CONSTRAINTS
-- ============================================

ALTER TABLE credit_accounts
ALTER COLUMN grace_period_days SET NOT NULL,
ALTER COLUMN minimum_due_percent SET NOT NULL,
ALTER COLUMN late_fee_amount SET NOT NULL,
ALTER COLUMN apr SET NOT NULL;

-- ============================================
-- STEP 5: ADD CHECK CONSTRAINTS (GOOD PRACTICE)
-- ============================================

ALTER TABLE credit_accounts
ADD CONSTRAINT chk_credit_accounts_grace_positive
CHECK (grace_period_days > 0),

ADD CONSTRAINT chk_credit_accounts_min_due_percent
CHECK (minimum_due_percent > 0),

ADD CONSTRAINT chk_credit_accounts_apr_positive
CHECK (apr >= 0);