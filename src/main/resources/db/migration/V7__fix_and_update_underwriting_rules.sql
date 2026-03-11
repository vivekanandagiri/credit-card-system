-- =====================================================
-- V7__fix_and_add_granular_underwriting_rules.sql
--
--
-- Changes:
-- 1. Removes all old RISK_SCORING rules from V6 seed
-- 2. Removes old FRAUD rule from V6 seed
-- 3. Inserts granular scoring rules across 6 dimensions
-- 4. Inserts granular fraud rules
--
-- SCORING DESIGN:
-- Base score = 0  (set in RiskScoringService.java)
-- Score range: 0 to 100
-- All score_impacts are POSITIVE — higher = safer applicant
--
-- DecisionEngine thresholds (unchanged):
--   >= 80 → AUTO_APPROVED
--   60-79 → PENDING_REVIEW
--   <  60 → AUTO_REJECTED
--
-- Score budget per dimension:
--   Credit Score Band     → max +30
--   Debt Burden Band      → max +20
--   Income Band           → max +20
--   Employment Band       → max +15
--   Limit/Income Band     → max +10
--   Liabilities Band      → max +10
--   ─────────────────────────────
--   Total possible        → 105 → clamped to 100
-- =====================================================


DELETE FROM underwriting_rules
WHERE rule_type IN ('RISK_SCORING', 'FRAUD');

INSERT INTO underwriting_rules
    (rule_name, rule_type, field_name, operator, threshold_value,
     action, score_impact, priority, rule_group, is_active, created_by, updated_by)
VALUES

-- ─────────────────────────────────────────────────────
-- 1. CREDIT SCORE BAND
-- Max contribution: +30
-- Highest threshold first (priority 10 fires before 11, 12, 13)
-- Score 810 → only priority 10 fires (+30), rest skipped
-- Score 760 → priority 10 fails, priority 11 fires (+25), rest skipped
-- Score 720 → priority 10,11 fail, priority 12 fires (+18), rest skipped
-- Score 670 → priority 10,11,12 fail, priority 13 fires (+10)
-- ─────────────────────────────────────────────────────
('Credit Score - Exceptional (800+)',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '800',
    'SCORE', 30, 10, 'CREDIT_SCORE_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Credit Score - Excellent (750-799)',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '750',
    'SCORE', 25, 11, 'CREDIT_SCORE_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Credit Score - Good (700-749)',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '700',
    'SCORE', 18, 12, 'CREDIT_SCORE_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Credit Score - Fair (650-699)',
    'RISK_SCORING', 'credit_score_at_application', 'GTE', '650',
    'SCORE', 10, 13, 'CREDIT_SCORE_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- 2. DEBT BURDEN BAND (existing_liabilities / monthly_income)
-- Max contribution: +20
-- Highest threshold first
-- Ratio 0.65 → priority 20 fires (+2), rest skipped
-- Ratio 0.55 → priority 20 fails, priority 21 fires (+5), rest skipped
-- Ratio 0.40 → priority 20,21 fail, priority 22 fires (+10), rest skipped
-- Ratio 0.20 → priority 20,21,22 fail, priority 23 fires (+15), rest skipped
-- Ratio 0.05 → priority 20-23 fail, priority 24 fires (+20)
-- ─────────────────────────────────────────────────────
('Debt Burden - Very High (>0.60)',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.60',
    'SCORE', 2, 20, 'DEBT_BURDEN_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Debt Burden - High (0.50-0.60)',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.50',
    'SCORE', 5, 21, 'DEBT_BURDEN_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Debt Burden - Moderate (0.30-0.50)',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.30',
    'SCORE', 10, 22, 'DEBT_BURDEN_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Debt Burden - Low (0.10-0.30)',
    'RISK_SCORING', 'debt_burden_ratio', 'GT', '0.10',
    'SCORE', 15, 23, 'DEBT_BURDEN_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Debt Burden - Very Low (<=0.10)',
    'RISK_SCORING', 'debt_burden_ratio', 'LTE', '0.10',
    'SCORE', 20, 24, 'DEBT_BURDEN_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- 3. INCOME BAND (annual_income = monthly_income * 12)
-- Max contribution: +20
-- Highest threshold first
-- ─────────────────────────────────────────────────────
('Income - Ultra High (>36L)',
    'RISK_SCORING', 'annual_income', 'GT', '3600000',
    'SCORE', 20, 30, 'INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Income - Very High (24L-36L)',
    'RISK_SCORING', 'annual_income', 'GT', '2400000',
    'SCORE', 18, 31, 'INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Income - High (12L-24L)',
    'RISK_SCORING', 'annual_income', 'GT', '1200000',
    'SCORE', 15, 32, 'INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Income - Medium (6L-12L)',
    'RISK_SCORING', 'annual_income', 'GT', '600000',
    'SCORE', 10, 33, 'INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Income - Low (3L-6L)',
    'RISK_SCORING', 'annual_income', 'GTE', '300000',
    'SCORE', 5, 34, 'INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- 4. EMPLOYMENT TYPE BAND
-- Max contribution: +15
-- EQ rules — only one will ever match per application
-- Group prevents hypothetical future multi-match issues
-- ─────────────────────────────────────────────────────
('Employment - Salaried',
    'RISK_SCORING', 'employment_type', 'EQ', 'SALARIED',
    'SCORE', 15, 40, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Employment - Business Owner',
    'RISK_SCORING', 'employment_type', 'EQ', 'BUSINESS_OWNER',
    'SCORE', 12, 41, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Employment - Self Employed',
    'RISK_SCORING', 'employment_type', 'EQ', 'SELF_EMPLOYED',
    'SCORE', 10, 42, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Employment - Freelancer',
    'RISK_SCORING', 'employment_type', 'EQ', 'FREELANCER',
    'SCORE', 7, 43, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Employment - Retired',
    'RISK_SCORING', 'employment_type', 'EQ', 'RETIRED',
    'SCORE', 8, 44, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Employment - Student',
    'RISK_SCORING', 'employment_type', 'EQ', 'STUDENT',
    'SCORE', 3, 45, 'EMPLOYMENT_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- 5. LIMIT TO INCOME BAND (requested_limit / annual_income)
-- Max contribution: +10
-- Lower ratio = more conservative request = safer
-- Lowest threshold first (LTE rules — most conservative fires first)
-- ─────────────────────────────────────────────────────
('Limit/Income - Very Conservative (<=0.10)',
    'RISK_SCORING', 'limit_to_income_ratio', 'LTE', '0.10',
    'SCORE', 10, 50, 'LIMIT_INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Limit/Income - Conservative (<=0.20)',
    'RISK_SCORING', 'limit_to_income_ratio', 'LTE', '0.20',
    'SCORE', 8, 51, 'LIMIT_INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Limit/Income - Moderate (<=0.35)',
    'RISK_SCORING', 'limit_to_income_ratio', 'LTE', '0.35',
    'SCORE', 5, 52, 'LIMIT_INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Limit/Income - Aggressive (<=0.50)',
    'RISK_SCORING', 'limit_to_income_ratio', 'LTE', '0.50',
    'SCORE', 2, 53, 'LIMIT_INCOME_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- 6. EXISTING LIABILITIES BAND (absolute rupee amount)
-- Max contribution: +10
-- Lowest threshold first (EQ/LTE — smallest fires first)
-- ─────────────────────────────────────────────────────
('Liabilities - None',
    'RISK_SCORING', 'existing_liabilities', 'EQ', '0',
    'SCORE', 10, 60, 'LIABILITIES_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Liabilities - Very Low (<=10000)',
    'RISK_SCORING', 'existing_liabilities', 'LTE', '10000',
    'SCORE', 8, 61, 'LIABILITIES_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Liabilities - Low (<=50000)',
    'RISK_SCORING', 'existing_liabilities', 'LTE', '50000',
    'SCORE', 5, 62, 'LIABILITIES_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Liabilities - Moderate (<=100000)',
    'RISK_SCORING', 'existing_liabilities', 'LTE', '100000',
    'SCORE', 3, 63, 'LIABILITIES_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

('Liabilities - High (>100000)',
    'RISK_SCORING', 'existing_liabilities', 'GT', '100000',
    'SCORE', 1, 64, 'LIABILITIES_BAND', TRUE, 'SYSTEM', 'SYSTEM'),

-- ─────────────────────────────────────────────────────
-- FRAUD RULES (granular — NULL group, fire independently)
-- ─────────────────────────────────────────────────────
('Fraud - Limit Exceeds 60% of Income',
    'FRAUD', 'limit_to_income_ratio', 'GT', '0.60',
    'FLAG_REVIEW', 0, 70, NULL, TRUE, 'SYSTEM', 'SYSTEM'),

('Fraud - Debt Burden Exceeds 70%',
    'FRAUD', 'debt_burden_ratio', 'GT', '0.70',
    'FLAG_REVIEW', 0, 71, NULL, TRUE, 'SYSTEM', 'SYSTEM'),

('Fraud - Extreme Debt Burden Exceeds 80%',
    'FRAUD', 'debt_burden_ratio', 'GT', '0.80',
    'FLAG_REVIEW', 0, 72, NULL, TRUE, 'SYSTEM', 'SYSTEM')

ON CONFLICT (rule_name) DO NOTHING;