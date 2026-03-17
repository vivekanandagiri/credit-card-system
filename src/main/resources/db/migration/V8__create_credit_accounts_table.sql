
-- ENUMS

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status_enum') THEN
        CREATE TYPE account_status_enum AS ENUM (
            'ACTIVE', 'SUSPENDED', 'BLOCKED', 'CLOSED'
        );
    END IF;
END$$;


-- NATIVE SEQUENCE FOR ACCOUNT NUMBER GENERATION

CREATE SEQUENCE IF NOT EXISTS account_number_seq
    START     1
    INCREMENT 1
    MINVALUE  1
    MAXVALUE  999999
    NO CYCLE;


-- ACCOUNTS TABLE
CREATE TABLE IF NOT EXISTS credit_accounts (
    account_id              UUID                    PRIMARY KEY DEFAULT uuid_generate_v4(),

    account_number          VARCHAR(12)             NOT NULL UNIQUE,

    customer_id             UUID                    NOT NULL,
    application_id          UUID                    NOT NULL UNIQUE,   
    card_product_id         UUID                    NOT NULL,

    -- Status
    account_status          account_status_enum     NOT NULL DEFAULT 'ACTIVE',

    -- Credit terms
    credit_limit            DECIMAL(19,4)           NOT NULL,
    apr                     DECIMAL(5,2)            NOT NULL,

    -- Live financial state
    current_balance         DECIMAL(19,4)           NOT NULL DEFAULT 0.0000,
    available_balance       DECIMAL(19,4)           NOT NULL,

    -- Billing
    statement_cycle_day     INT                     NOT NULL,
    last_statement_date     DATE,
    last_statement_balance  DECIMAL(19,4),
    next_due_date           DATE,
    minimum_due_amount      DECIMAL(19,4)           NOT NULL DEFAULT 0.0000,
    last_payment_date       DATE,
    last_payment_amount     DECIMAL(19,4),

    -- Lifecycle
    activated_at            TIMESTAMPTZ             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at               TIMESTAMPTZ,

    -- Audit
    created_at              TIMESTAMPTZ             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100)            NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100)            NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT fk_account_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE RESTRICT,

    CONSTRAINT fk_account_application
        FOREIGN KEY (application_id)
        REFERENCES credit_card_applications(application_id) ON DELETE RESTRICT,

    CONSTRAINT fk_account_card_product
        FOREIGN KEY (card_product_id)
        REFERENCES credit_card_products(card_product_id) ON DELETE RESTRICT,

    CONSTRAINT chk_credit_limit        CHECK (credit_limit > 0),
    CONSTRAINT chk_current_balance     CHECK (current_balance >= 0),
    CONSTRAINT chk_available_balance   CHECK (available_balance >= 0),
    CONSTRAINT chk_statement_cycle_day CHECK (statement_cycle_day BETWEEN 1 AND 28),
    CONSTRAINT chk_apr                 CHECK (apr >= 0)
);


-- INDEXES

CREATE INDEX IF NOT EXISTS idx_account_customer        ON credit_accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_account_status          ON credit_accounts(account_status);
CREATE INDEX IF NOT EXISTS idx_account_card_product    ON credit_accounts(card_product_id);
CREATE INDEX IF NOT EXISTS idx_account_number          ON credit_accounts(account_number);