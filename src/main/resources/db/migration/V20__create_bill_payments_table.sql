-- Stores one record per payment made against a statement.
-- Supports partial payments and audit tracking.

-- 1. Payment Method Enum
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_enum') THEN
        CREATE TYPE payment_method_enum AS ENUM (
            'NET_BANKING',
            'UPI',
            'NEFT',
            'RTGS',
            'DEBIT_CARD'
        );
    END IF;
END$$;

-- 2. Payment Status Enum
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status_enum') THEN
        CREATE TYPE payment_status_enum AS ENUM (
            'SUCCESS',
            'FAILED',
            'PENDING'
        );
    END IF;
END$$;

-- 3. Payments Table
CREATE TABLE IF NOT EXISTS payments (

    payment_id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Relations
    statement_id        UUID NOT NULL,
    account_id          UUID NOT NULL,

    -- Payment details
    payment_amount      NUMERIC(19, 4) NOT NULL CHECK (payment_amount > 0),
    payment_method      payment_method_enum NOT NULL,
    payment_status      payment_status_enum NOT NULL DEFAULT 'SUCCESS',

    -- External reference (UPI ID, bank ref, etc.)
    reference_number    VARCHAR(100) NOT NULL,

    -- Balance snapshot (audit)
    balance_before      NUMERIC(19, 4) NOT NULL,
    balance_after       NUMERIC(19, 4) NOT NULL,

    -- Timestamp
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',

    -- Constraints
    CONSTRAINT fk_payment_statement
        FOREIGN KEY (statement_id)
        REFERENCES billing_statements(statement_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_payment_account
        FOREIGN KEY (account_id)
        REFERENCES credit_accounts(account_id)
        ON DELETE RESTRICT,

    -- Prevent duplicate external transaction references
    CONSTRAINT uq_payment_reference UNIQUE (reference_number, payment_method)
);

-- 4. Indexes (Performance)
CREATE INDEX IF NOT EXISTS idx_payment_statement 
    ON payments(statement_id);

CREATE INDEX IF NOT EXISTS idx_payment_account   
    ON payments(account_id);

CREATE INDEX IF NOT EXISTS idx_payment_status    
    ON payments(payment_status);

CREATE INDEX IF NOT EXISTS idx_payment_paid_at   
    ON payments(paid_at);