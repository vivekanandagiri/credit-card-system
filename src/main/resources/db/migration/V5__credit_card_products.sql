CREATE TYPE card_type_enum AS ENUM ('PHYSICAL', 'VIRTUAL');
CREATE TYPE card_network_enum AS ENUM (
    'VISA',
    'MASTERCARD',
    'RUPAY',
    'AMEX',
    'DINERS'
);

CREATE TABLE credit_card_products (
    card_product_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    credit_product_id BIGINT NOT NULL,
    CONSTRAINT fk_card_product_credit_product
        FOREIGN KEY (credit_product_id)
        REFERENCES credit_products(credit_product_id)
        ON DELETE RESTRICT,

    product_name VARCHAR(150) NOT NULL,

    network_type card_network_enum NOT NULL,
    card_type card_type_enum NOT NULL,

    annual_fee NUMERIC(19,4) NOT NULL CHECK (annual_fee >= 0),
    card_validity_years INT NOT NULL CHECK (card_validity_years > 0),

    contactless_enabled BOOLEAN NOT NULL,
    international_usage_allowed BOOLEAN NOT NULL,
    online_transactions_allowed BOOLEAN NOT NULL,
    atm_withdrawal_allowed BOOLEAN NOT NULL,

    atm_daily_limit NUMERIC(19,4) NOT NULL CHECK (atm_daily_limit >= 0),
    pos_daily_limit NUMERIC(19,4) NOT NULL CHECK (pos_daily_limit >= 0),
    ecommerce_daily_limit NUMERIC(19,4) NOT NULL CHECK (ecommerce_daily_limit >= 0),

    statement_cycle_day INT NOT NULL
        CHECK (statement_cycle_day BETWEEN 1 AND 28),

    forex_markup_percent NUMERIC(5,2) NOT NULL
        CHECK (forex_markup_percent BETWEEN 0 AND 100),

    product_description TEXT,
    status product_status_enum NOT NULL DEFAULT 'ACTIVE',


    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
CREATE INDEX idx_card_product_credit_product
ON credit_card_products(credit_product_id);
CREATE INDEX idx_card_product_status
ON credit_card_products(status);