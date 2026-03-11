-- =====================================================
-- PRODUCT STATUS ENUM
-- =====================================================
CREATE TYPE product_status_enum AS ENUM ('ACTIVE', 'INACTIVE');


-- =====================================================
-- CREDIT PRODUCTS
-- =====================================================
CREATE TABLE credit_products (
    credit_product_id BIGSERIAL PRIMARY KEY,

    product_code VARCHAR(50) NOT NULL UNIQUE,
    product_name VARCHAR(150) NOT NULL,

    min_credit_limit NUMERIC(19,4) NOT NULL,
    max_credit_limit NUMERIC(19,4) NOT NULL,
    min_income_required NUMERIC(19,4) NOT NULL,
    min_credit_score INT NOT NULL,

    apr_purchase NUMERIC(5,2) NOT NULL,
    apr_cash_advance NUMERIC(5,2) NOT NULL,
    grace_period_days INT NOT NULL,
    interest_calculation_method VARCHAR(50) NOT NULL,

    minimum_due_percent NUMERIC(5,2) NOT NULL,
    minimum_due_amount NUMERIC(19,4) NOT NULL,

    late_fee_amount NUMERIC(19,4) NOT NULL,
    overlimit_fee NUMERIC(19,4) NOT NULL,
    joining_fee NUMERIC(19,4) NOT NULL,
    foreign_transaction_fee_percent NUMERIC(5,2) NOT NULL,
    balance_transfer_fee_percent NUMERIC(5,2) NOT NULL,
    cash_advance_fee_percent NUMERIC(5,2) NOT NULL,
    cash_advance_fee_min NUMERIC(19,4) NOT NULL,
    
    
	effective_from  DATE                NOT NULL,
    effective_to    DATE,
    status product_status_enum NOT NULL DEFAULT 'ACTIVE',

	created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    --constraints
    CONSTRAINT chk_credit_limit CHECK (min_credit_limit < max_credit_limit),
    CONSTRAINT chk_credit_score CHECK (min_credit_score > 0),
    CONSTRAINT chk_apr_purchase CHECK (apr_purchase >= 0),
    CONSTRAINT chk_apr_cash CHECK (apr_cash_advance >= 0),
    CONSTRAINT chk_grace_period CHECK (grace_period_days >= 0)
);