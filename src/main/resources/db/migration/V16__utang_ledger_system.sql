-- Flyway Migration V16: Utang (Credit) Ledger System
-- Implements customer credit lines, transactions, payments, and credit scoring
-- for Philippine sari-sari store operations

-- 1. Extend crm_customers table with credit tracking
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS has_credit_line BOOLEAN DEFAULT FALSE;
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_limit DECIMAL(15, 2) DEFAULT 0;
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS total_utang DECIMAL(15, 2) DEFAULT 0;
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_status VARCHAR(30) DEFAULT 'INACTIVE';

-- 2. Create customer_ledgers table - tracks credit line per customer
CREATE TABLE IF NOT EXISTS customer_ledgers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    credit_limit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    available_credit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_outstanding DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, CLOSED
    credit_line_approved_at TIMESTAMP,
    credit_line_approved_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, customer_id)
);

-- 3. Create utang_transactions table - ledger of all credit/debit events
CREATE TABLE IF NOT EXISTS utang_transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    customer_ledger_id BIGINT NOT NULL REFERENCES customer_ledgers(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL, -- DEBIT (sale), CREDIT (payment), ADJUSTMENT, PENALTY
    reference_type VARCHAR(50), -- ORDER, PAYMENT, ADJUSTMENT
    reference_id BIGINT,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    due_date TIMESTAMP,
    notes TEXT,
    created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- soft delete for audit trail
);

-- 4. Create utang_payments table - track payment records against utang
CREATE TABLE IF NOT EXISTS utang_payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- CASH, CHEQUE, TRANSFER, GCASH, INSTALLMENT
    reference_number VARCHAR(100),
    notes TEXT,
    received_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- soft delete
);

-- 5. Create utang_payment_allocations - which transactions does this payment cover?
CREATE TABLE IF NOT EXISTS utang_payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    payment_id BIGINT NOT NULL REFERENCES utang_payments(id) ON DELETE CASCADE,
    transaction_id BIGINT NOT NULL REFERENCES utang_transactions(id) ON DELETE CASCADE,
    amount_allocated DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Create customer_credit_scores table - calculated reputation score
CREATE TABLE IF NOT EXISTS customer_credit_scores (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    score DECIMAL(5, 2) DEFAULT 0, -- 0-100 scale
    on_time_payments INT DEFAULT 0,
    late_payments INT DEFAULT 0,
    missed_payments INT DEFAULT 0,
    total_transactions INT DEFAULT 0,
    payment_percentage DECIMAL(5, 2) DEFAULT 0, -- % of utang paid on time
    last_calculated_at TIMESTAMP,
    calculated_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, customer_id)
);

-- 7. Create utang_reminders table - track payment reminders and follow-ups
CREATE TABLE IF NOT EXISTS utang_reminders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    amount_due DECIMAL(15, 2) NOT NULL,
    reminder_type VARCHAR(30) NOT NULL, -- DUE_DATE_APPROACHING, OVERDUE, PENALTY_NOTICE
    reminder_date TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, ACKNOWLEDGED, RESOLVED
    sent_via VARCHAR(30), -- SMS, EMAIL, CALL, IN_PERSON
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_customer_ledgers_tenant_customer ON customer_ledgers(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_ledgers_status ON customer_ledgers(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_utang_transactions_tenant_customer ON utang_transactions(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_utang_transactions_type_created ON utang_transactions(tenant_id, transaction_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_utang_transactions_order_id ON utang_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_utang_transactions_due_date ON utang_transactions(tenant_id, due_date) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_utang_payments_tenant_customer ON utang_payments(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_utang_payments_date ON utang_payments(tenant_id, payment_date DESC);

CREATE INDEX IF NOT EXISTS idx_payment_allocations_payment_id ON utang_payment_allocations(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_allocations_transaction_id ON utang_payment_allocations(transaction_id);

CREATE INDEX IF NOT EXISTS idx_credit_scores_tenant_score ON customer_credit_scores(tenant_id, score DESC);

CREATE INDEX IF NOT EXISTS idx_utang_reminders_tenant_status ON utang_reminders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_utang_reminders_due_date ON utang_reminders(tenant_id, reminder_date);
