-- Flyway Migration V5: Payments Module
-- Creates provider-neutral payment tables and links payment state to orders.

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(120),
    provider_checkout_id VARCHAR(120),
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    intent_type VARCHAR(30) NOT NULL,
    checkout_url VARCHAR(2048),
    idempotency_key VARCHAR(120) NOT NULL,
    failure_code VARCHAR(120),
    failure_message VARCHAR(1024),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_payments_provider_payment_id UNIQUE (provider, provider_payment_id),
    CONSTRAINT uk_payments_provider_checkout_id UNIQUE (provider, provider_checkout_id)
);

CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT REFERENCES payments(id) ON DELETE SET NULL,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL,
    event_type VARCHAR(160) NOT NULL,
    raw_payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processing_status VARCHAR(30) NOT NULL,
    CONSTRAINT uk_payment_events_provider_event UNIQUE (provider, provider_event_id)
);

CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    request_type VARCHAR(40) NOT NULL,
    provider_reference_id VARCHAR(160),
    status VARCHAR(30) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_provider_status ON payments(provider, status);
CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX idx_payment_events_received_at ON payment_events(received_at DESC);
CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts(payment_id);
