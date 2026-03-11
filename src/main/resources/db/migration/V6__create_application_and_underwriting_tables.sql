-- V6__create_application_and_underwriting_tables.sql
-- ENUMS

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'application_status_enum') THEN
        CREATE TYPE application_status_enum AS ENUM (
            'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PENDING_REVIEW'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'decision_type_enum') THEN
        CREATE TYPE decision_type_enum AS ENUM (
            'AUTO_APPROVED', 'AUTO_REJECTED',
            'MANUALLY_APPROVED', 'MANUALLY_REJECTED',
            'PENDING_REVIEW'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'employment_type_enum') THEN
        CREATE TYPE employment_type_enum AS ENUM (
            'SALARIED', 'SELF_EMPLOYED', 'BUSINESS_OWNER',
            'FREELANCER', 'STUDENT', 'RETIRED', 'UNEMPLOYED'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'rule_type_enum') THEN
        CREATE TYPE rule_type_enum AS ENUM (
            'ELIGIBILITY', 'RISK_SCORING', 'FRAUD'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'rule_action_enum') THEN
        CREATE TYPE rule_action_enum AS ENUM (
            'APPROVE', 'REJECT', 'SCORE', 'FLAG_REVIEW'
        );
    END IF;
END$$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'rule_operator_enum') THEN
        CREATE TYPE rule_operator_enum AS ENUM (
            'GT', 'GTE', 'LT', 'LTE', 'EQ', 'NEQ'
        );
    END IF;
END$$;


-- CREDIT CARD APPLICATIONS TABLE
CREATE TABLE IF NOT EXISTS credit_card_applications (
    application_id              UUID                        PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id                 UUID                        NOT NULL,
    card_product_id             UUID                        NOT NULL,

    -- Customer provided data
    employment_type             employment_type_enum        NOT NULL,
    employer_name               VARCHAR(200),
    monthly_income              DECIMAL(19,4)               NOT NULL,
    existing_liabilities        DECIMAL(19,4)               NOT NULL DEFAULT 0,
    credit_score_at_application INT                         NOT NULL,
    requested_credit_limit      DECIMAL(19,4)               NOT NULL,

    -- Underwriting output
    application_status          application_status_enum     NOT NULL DEFAULT 'SUBMITTED',
    risk_score                  DECIMAL(5,2),
    decision                    decision_type_enum,
    decision_reason             VARCHAR(500),
    approved_credit_limit       DECIMAL(19,4),
    approved_apr                DECIMAL(5,2),

    -- Timestamps
    submitted_at                TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decision_at                 TIMESTAMPTZ,

    -- Audit
    created_at                  TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(100)                NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(100)                NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT fk_application_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_application_card_product
        FOREIGN KEY (card_product_id) REFERENCES credit_card_products(card_product_id) ON DELETE RESTRICT,

    CONSTRAINT chk_monthly_income   CHECK (monthly_income > 0),
    CONSTRAINT chk_liabilities      CHECK (existing_liabilities >= 0),
    CONSTRAINT chk_credit_score     CHECK (credit_score_at_application BETWEEN 300 AND 900),
    CONSTRAINT chk_requested_limit  CHECK (requested_credit_limit > 0)
);


-- UNDERWRITING RULES TABLE
CREATE TABLE IF NOT EXISTS underwriting_rules (
    rule_id         UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name       VARCHAR(150)        NOT NULL UNIQUE,
    rule_type       rule_type_enum      NOT NULL,
    field_name      VARCHAR(100)        NOT NULL,
    operator        rule_operator_enum  NOT NULL,
    threshold_value VARCHAR(100)        NOT NULL,
    action          rule_action_enum    NOT NULL,
    score_impact    DECIMAL(5,2)        NOT NULL DEFAULT 0,
    priority        INT                 NOT NULL DEFAULT 0,
    rule_group      VARCHAR(100),
    is_active       BOOLEAN             NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',

    CONSTRAINT chk_priority CHECK (priority >= 0)
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_application_customer     ON credit_card_applications(customer_id);
CREATE INDEX IF NOT EXISTS idx_application_status       ON credit_card_applications(application_status);
CREATE INDEX IF NOT EXISTS idx_application_card_product ON credit_card_applications(card_product_id);
CREATE INDEX IF NOT EXISTS idx_rules_type_active        ON underwriting_rules(rule_type, is_active);
CREATE INDEX IF NOT EXISTS idx_rules_priority           ON underwriting_rules(priority);
CREATE INDEX IF NOT EXISTS idx_rules_group              ON underwriting_rules(rule_group);

-- SEED UNDERWRITING RULES
INSERT INTO underwriting_rules
    (rule_name, rule_type, field_name, operator, threshold_value, action, score_impact, priority, rule_group)
VALUES

-- ELIGIBILITY (hard stop — NULL group, fire independently)
('Minimum Credit Score',
    'ELIGIBILITY', 'credit_score_at_application', 'LT', '650',
    'REJECT', 0, 1, NULL),

('Minimum Annual Income',
    'ELIGIBILITY', 'annual_income', 'LT', '300000',
    'REJECT', 0, 2, NULL),

('Unemployed Rejection',
    'ELIGIBILITY', 'employment_type', 'EQ', 'UNEMPLOYED',
    'REJECT', 0, 3, NULL),

-- CREDIT SCORE BAND (highest threshold first → priority 10, 11, 12)
('Excellent Credit Score',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '750',
    'SCORE', 20, 10, 'CREDIT_SCORE_BAND'),

('Good Credit Score',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '700',
    'SCORE', 10, 11, 'CREDIT_SCORE_BAND'),

('Poor Credit Score',
    'RISK_SCORING', 'credit_score_at_application', 'LT', '700',
    'SCORE', -5, 12, 'CREDIT_SCORE_BAND'),

-- DEBT BURDEN BAND (highest threshold first → priority 20, 21, 22)
('High Debt Burden',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.50',
    'SCORE', -20, 20, 'DEBT_BURDEN_BAND'),

('Moderate Debt Burden',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.30',
    'SCORE', -10, 21, 'DEBT_BURDEN_BAND'),

('Low Debt Burden',
    'RISK_SCORING', 'debt_burden_ratio', 'LTE', '0.30',
    'SCORE', 15, 22, 'DEBT_BURDEN_BAND'),

-- INDEPENDENT SCORING RULES (NULL group — always fire if matched)
('Salaried Employment Bonus',
    'RISK_SCORING', 'employment_type', 'EQ', 'SALARIED',
    'SCORE', 10, 30, NULL),

('High Income Bonus',
    'RISK_SCORING', 'annual_income', 'GTE', '1200000',
    'SCORE', 10, 31, NULL),

-- FRAUD
('Requested Limit Too High',
    'FRAUD', 'limit_to_income_ratio', 'GT', '0.50',
    'FLAG_REVIEW', 0, 40, NULL)

ON CONFLICT (rule_name) DO NOTHING;