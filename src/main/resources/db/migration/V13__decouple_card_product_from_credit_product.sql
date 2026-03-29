
-- Step 1: Drop FK constraint
ALTER TABLE credit_card_products
    DROP CONSTRAINT IF EXISTS fk_card_product_credit_product;

-- Step 2: Drop index
DROP INDEX IF EXISTS idx_card_product_credit_product;

-- Step 3: Drop the column
ALTER TABLE credit_card_products
    DROP COLUMN IF EXISTS credit_product_id;



-- 1. UPDATE transaction_type_enum

DO $$ BEGIN
    ALTER TYPE transaction_type_enum ADD VALUE IF NOT EXISTS 'REFUND';
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    ALTER TYPE transaction_type_enum ADD VALUE IF NOT EXISTS 'PAYMENT';
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    ALTER TYPE transaction_type_enum ADD VALUE IF NOT EXISTS 'FEE';
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    ALTER TYPE transaction_type_enum ADD VALUE IF NOT EXISTS 'INTEREST';
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 2. CREATE transaction_channel_enum

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_channel_enum') THEN
        CREATE TYPE transaction_channel_enum AS ENUM (
            'ONLINE',
            'POS',
            'ATM'
        );
    END IF;
END$$;

-- 3. ADD COLUMN transaction_channel


ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS transaction_channel transaction_channel_enum;


-- Set default channel

ALTER TABLE transactions
    ALTER COLUMN transaction_channel SET DEFAULT 'ONLINE';
