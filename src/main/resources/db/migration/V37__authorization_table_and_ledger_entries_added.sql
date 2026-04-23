-- V37: Ledger + Authorization (FINAL CLEAN)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. ENUMS

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ledger_entry_type') THEN
        CREATE TYPE ledger_entry_type AS ENUM ('DEBIT', 'CREDIT');
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auth_status') THEN
        CREATE TYPE auth_status AS ENUM (
            'AUTHORIZED',
            'CAPTURED',
            'EXPIRED',
            'REVERSED'
        );
    END IF;
END$$;

-- 2. RENAME COLUMNS (SAFE)

DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='transactions' AND column_name='reference_number'
    ) THEN
        ALTER TABLE transactions RENAME COLUMN reference_number TO internal_reference;
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='transactions' AND column_name='transaction_reference'
    ) THEN
        ALTER TABLE transactions RENAME COLUMN transaction_reference TO network_reference;
    END IF;
END$$;

-- 3. AUTHORIZATIONS TABLE

CREATE TABLE IF NOT EXISTS authorizations (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    account_id UUID NOT NULL,
    card_id UUID,

    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),

    status auth_status NOT NULL,

    network_reference VARCHAR(100) UNIQUE,

    expires_at TIMESTAMP NOT NULL,

    -- JPA will populate these (NO DEFAULT)
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(100),

    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_auth_account
ON authorizations(account_id);

-- 4. TRANSACTION CHANGES

ALTER TABLE transactions
ADD COLUMN IF NOT EXISTS authorization_id UUID;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_txn_authorization'
    ) THEN
        ALTER TABLE transactions
        ADD CONSTRAINT fk_txn_authorization
        FOREIGN KEY (authorization_id)
        REFERENCES authorizations(id);
    END IF;
END$$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_txn_network_ref
ON transactions(network_reference)
WHERE network_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_txn_account
ON transactions(credit_account_id);

CREATE INDEX IF NOT EXISTS idx_txn_type_status
ON transactions(transaction_type, transaction_status);

-- 5. LEDGER TABLE

CREATE TABLE IF NOT EXISTS ledger_entries (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    account_id UUID NOT NULL,

    entry_type ledger_entry_type NOT NULL,

    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),

    reference_type VARCHAR(20) NOT NULL,
    reference_id UUID NOT NULL,

    -- JPA auditing fields
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(100),

    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ledger_account
ON ledger_entries(account_id);

CREATE INDEX IF NOT EXISTS idx_ledger_reference
ON ledger_entries(reference_type, reference_id);

-- 6. SAFE BACKFILL (IDEMPOTENT)

-- DEBIT
INSERT INTO ledger_entries (
    account_id,
    entry_type,
    amount,
    reference_type,
    reference_id,
    created_at,
    updated_at
)
SELECT
    t.credit_account_id,
    'DEBIT',
    t.amount,
    'TRANSACTION',
    t.transaction_id,
    t.created_at,
    t.created_at
FROM transactions t
WHERE t.transaction_type IN ('PURCHASE', 'FEE', 'INTEREST')
  AND t.transaction_status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM ledger_entries l
      WHERE l.reference_id = t.transaction_id
        AND l.reference_type = 'TRANSACTION'
  );

-- CREDIT
INSERT INTO ledger_entries (
    account_id,
    entry_type,
    amount,
    reference_type,
    reference_id,
    created_at,
    updated_at
)
SELECT
    t.credit_account_id,
    'CREDIT',
    t.amount,
    'TRANSACTION',
    t.transaction_id,
    t.created_at,
    t.created_at
FROM transactions t
WHERE t.transaction_type IN ('PAYMENT', 'REFUND')
  AND t.transaction_status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM ledger_entries l
      WHERE l.reference_id = t.transaction_id
        AND l.reference_type = 'TRANSACTION'
  );