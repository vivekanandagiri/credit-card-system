
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_type_enum') THEN
        CREATE TYPE transaction_type_enum AS ENUM (
            'PURCHASE', 
            'ONLINE'     
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_status_enum') THEN
        CREATE TYPE transaction_status_enum AS ENUM (
            'APPROVED',  
            'DECLINED'   
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'currency_enum') THEN
        CREATE TYPE currency_enum AS ENUM (
            'INR',
            'USD',
            'EUR'
        );
    END IF;
END$$;


-- TRANSACTIONS TABLE

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id          UUID                        PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Card used for the transaction 
    card_id                 UUID                        NOT NULL,

    -- Credit Account 
    credit_account_id       UUID                        NOT NULL,

    -- Transaction details
    transaction_type        transaction_type_enum       NOT NULL,
    transaction_status      transaction_status_enum     NOT NULL,

    -- Amount
    amount                  NUMERIC(19, 4)              NOT NULL,
    currency 				currency_enum 				NOT NULL DEFAULT 'INR',

    -- Merchant info
    merchant_name           VARCHAR(200),
    merchant_category_code  VARCHAR(4),    
    merchant_category_name  VARCHAR(100), 
    
 
    -- Captures balance before and after
    balance_before          NUMERIC(19, 4)              NOT NULL,
    balance_after           NUMERIC(19, 4)              NOT NULL,

    decline_reason          VARCHAR(500),

    -- Unique reference number for this transaction
    reference_number        VARCHAR(50)                 NOT NULL UNIQUE,

    -- Timestamps
    transaction_time        TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at              TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100)                NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100)                NOT NULL DEFAULT 'SYSTEM',

    -- Constraints
    CONSTRAINT fk_transaction_card
        FOREIGN KEY (card_id)
        REFERENCES cards(card_id) ON DELETE RESTRICT,

    CONSTRAINT fk_transaction_account
        FOREIGN KEY (credit_account_id)
        REFERENCES credit_accounts(account_id) ON DELETE RESTRICT,

    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_merchant_category_code
        CHECK (merchant_category_code ~ '^\d{4}$' OR merchant_category_code IS NULL)
);

-- INDEXES

CREATE INDEX IF NOT EXISTS idx_txn_card          ON transactions(card_id);
CREATE INDEX IF NOT EXISTS idx_txn_account       ON transactions(credit_account_id);
CREATE INDEX IF NOT EXISTS idx_txn_status        ON transactions(transaction_status);
CREATE INDEX IF NOT EXISTS idx_txn_type          ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_txn_time          ON transactions(transaction_time);
CREATE INDEX IF NOT EXISTS idx_txn_reference     ON transactions(reference_number);

-- Composite index for daily limit queries:
-- "sum of APPROVED transactions for this card today of this type"
CREATE INDEX IF NOT EXISTS idx_txn_card_type_time
    ON transactions(card_id, transaction_type, transaction_time)
    WHERE transaction_status = 'APPROVED';