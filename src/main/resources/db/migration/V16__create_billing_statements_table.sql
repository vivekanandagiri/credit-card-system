
-- Stores one statement per account per billing cycle.

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'statement_status_enum') THEN
        CREATE TYPE statement_status_enum AS ENUM (
            'GENERATED',   -- statement created, payment not yet due
            'DUE',         -- past due date, payment not received
            'PAID',        -- fully paid
            'PARTIALLY_PAID', -- partially paid
            'OVERDUE'      -- past due date with outstanding balance
        );
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS billing_statements (

    statement_id            UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),


    account_id              UUID            NOT NULL,


    billing_period_start    DATE            NOT NULL,
    billing_period_end      DATE            NOT NULL,   -- the statement_cycle_day date

    -- Balances
    opening_balance         NUMERIC(19, 4)  NOT NULL,   -- current_balance at period start
    total_transactions      NUMERIC(19, 4)  NOT NULL,   -- sum of APPROVED txns in period
    closing_balance         NUMERIC(19, 4)  NOT NULL,   -- opening + total_transactions
    total_amount_due        NUMERIC(19, 4)  NOT NULL,   -- same as closing_balance


    minimum_due_amount      NUMERIC(19, 4)  NOT NULL,
    min_due_percent         NUMERIC(5, 2)   NOT NULL,   -- e.g. 5.00 (%)
    min_due_floor           NUMERIC(19, 4)  NOT NULL,   -- e.g. 200.00 (₹)


    due_date                DATE            NOT NULL,


    amount_paid             NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    paid_at                 TIMESTAMPTZ,

    statement_status        statement_status_enum NOT NULL DEFAULT 'GENERATED',

    -- Audit
    generated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100)    NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100)    NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT fk_statement_account
        FOREIGN KEY (account_id)
        REFERENCES credit_accounts(account_id) ON DELETE RESTRICT,

    -- One statement per account per billing period end date
    CONSTRAINT uq_statement_account_period
        UNIQUE (account_id, billing_period_end)
);

CREATE INDEX IF NOT EXISTS idx_stmt_account      ON billing_statements(account_id);
CREATE INDEX IF NOT EXISTS idx_stmt_status       ON billing_statements(statement_status);
CREATE INDEX IF NOT EXISTS idx_stmt_due_date     ON billing_statements(due_date);
CREATE INDEX IF NOT EXISTS idx_stmt_period_end   ON billing_statements(billing_period_end);