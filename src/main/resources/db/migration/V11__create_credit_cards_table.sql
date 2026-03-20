

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'card_format_enum') THEN
        CREATE TYPE card_format_enum AS ENUM (
            'VIRTUAL',
            'PHYSICAL'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'card_status_enum') THEN
        CREATE TYPE card_status_enum AS ENUM (
            'PENDING_ACTIVATION',
            'ACTIVE',
            'BLOCKED',
            'EXPIRED',
            'CANCELLED'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'card_issuance_reason_enum') THEN
        CREATE TYPE card_issuance_reason_enum AS ENUM (
            'NEW_CARD',
            'REPLACEMENT',
            'UPGRADE'
        );
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS cards (
    card_id               UUID                      PRIMARY KEY DEFAULT uuid_generate_v4(),

    credit_account_id     UUID                      NOT NULL,
    card_product_id       UUID                      NOT NULL,
    customer_id           UUID                      NOT NULL,

    card_format           card_format_enum          NOT NULL,
    card_status           card_status_enum          NOT NULL DEFAULT 'PENDING_ACTIVATION',
    issuance_reason       card_issuance_reason_enum NOT NULL DEFAULT 'NEW_CARD',

    masked_card_number    VARCHAR(19)               NOT NULL UNIQUE,

    expiry_month          INT                       NOT NULL,
    expiry_year           INT                       NOT NULL,

    issued_at             TIMESTAMPTZ               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at          TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    blocked_at            TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,

    issued_by             VARCHAR(100)              NOT NULL DEFAULT 'SYSTEM',

    created_at            TIMESTAMPTZ               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100)              NOT NULL DEFAULT 'SYSTEM',
    updated_at            TIMESTAMPTZ               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(100)              NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT fk_card_credit_account
        FOREIGN KEY (credit_account_id)
        REFERENCES credit_accounts(account_id) ON DELETE RESTRICT,

    CONSTRAINT fk_card_card_product
        FOREIGN KEY (card_product_id)
        REFERENCES credit_card_products(card_product_id) ON DELETE RESTRICT,

    CONSTRAINT fk_card_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE RESTRICT,

    CONSTRAINT chk_card_expiry_month
        CHECK (expiry_month BETWEEN 1 AND 12)

);

CREATE INDEX IF NOT EXISTS idx_card_credit_account ON cards(credit_account_id);
CREATE INDEX IF NOT EXISTS idx_card_customer       ON cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_card_status         ON cards(card_status);
CREATE INDEX IF NOT EXISTS idx_card_card_product   ON cards(card_product_id);
CREATE INDEX IF NOT EXISTS idx_card_format         ON cards(card_format);
CREATE INDEX IF NOT EXISTS idx_masked_card_number  ON cards(masked_card_number);