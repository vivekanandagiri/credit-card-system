-- Enable extension for UUID generation (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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
-- Create table
CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statement_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    reference_id VARCHAR(100),

    paid_at TIMESTAMP,
    
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT fk_payment_statement
        FOREIGN KEY (statement_id)
        REFERENCES billing_statements(statement_id)
        ON DELETE CASCADE
);

-- Create index
CREATE INDEX IF NOT EXISTS idx_payment_id
ON payments(statement_id);