-- Fix duplicate status columns in payments table
-- Standardize to: payment_status

BEGIN;

-- 1. Copy data from status → payment_status (if needed)
UPDATE payments
SET payment_status = status
WHERE payment_status IS NULL
  AND status IS NOT NULL;

-- 2. (Optional) Ensure no nulls remain
-- UPDATE payments SET payment_status = 'PENDING' WHERE payment_status IS NULL;

-- 3. Drop old column
ALTER TABLE payments
DROP COLUMN status;

COMMIT;