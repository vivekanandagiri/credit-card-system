-- =====================================================
-- EXTENSION
-- =====================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- =====================================================
-- ENUM
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role_enum') THEN
        CREATE TYPE user_role_enum AS ENUM ('ADMIN', 'CUSTOMER');
        CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE','OTHER');
        CREATE TYPE kyc_status AS ENUM (
						'PENDING',
    					'SUBMITTED',
  						  'VERIFIED',
  						  'REJECTED',
    				'RESUBMIT_REQUIRED'
);
    END IF;
END$$;


-- =====================================================
-- CUSTOMERS (Business Identity)
-- =====================================================
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    date_of_birth DATE NOT NULL,
    gender gender_enum NOT NULL,
    pan_number VARCHAR(20) NOT NULL UNIQUE,
    kyc_status kyc_status NOT NULL,
    

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL
);


-- =====================================================
-- USERS (Authentication Layer)
-- =====================================================
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- NULL for ADMIN, NOT NULL for CUSTOMER (enforced in service layer)
    customer_id UUID,

    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,

    role user_role_enum NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),

    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT fk_user_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 1–1 RELATIONSHIP ENFORCEMENT
-- One user per customer (only when customer_id is not null)
-- =====================================================
CREATE UNIQUE INDEX unique_user_per_customer
ON users(customer_id)
WHERE customer_id IS NOT NULL;


-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_mobile ON users(mobile_number);
CREATE INDEX idx_customers_email ON customers(email);