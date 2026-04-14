-- =========================================
-- Fix payments table (status + method)
-- =========================================

-- 1. Ensure status column exists and is NOT NULL
ALTER TABLE payments
ALTER COLUMN status SET NOT NULL;

-- 2. Ensure payment_method is NOT NULL (matches entity)
ALTER TABLE payments
ALTER COLUMN payment_method SET NOT NULL;

-- 3. Add CHECK constraint for status

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_payment_status'
    ) THEN
        ALTER TABLE payments
        ADD CONSTRAINT chk_payment_status
        CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'));
    END IF;
END$$;


-- 4. Add CHECK constraint for payment_method

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_payment_method'
    ) THEN
        ALTER TABLE payments
        ADD CONSTRAINT chk_payment_method
        CHECK (payment_method IN ('NET_BANKING', 'UPI', 'NEFT', 'RTGS', 'DEBIT_CARD'));
    END IF;
END$$;

-- 5. Clean up unused enum types
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status_enum') THEN
        DROP TYPE payment_status_enum;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_enum') THEN
        DROP TYPE payment_method_enum;
    END IF;
END$$;