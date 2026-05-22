-- Flyway Migration V9: Advance Ticketing

CREATE TABLE ticket_categories (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512),
    UNIQUE (tenant_id, name)
);

CREATE TABLE sla_policies (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    response_hours INTEGER NOT NULL DEFAULT 24,
    resolution_hours INTEGER NOT NULL DEFAULT 72,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES ticket_categories(id) ON DELETE SET NULL,
    customer_id BIGINT REFERENCES crm_customers(id) ON DELETE SET NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    requester_email VARCHAR(255),
    sla_policy_id BIGINT REFERENCES sla_policies(id) ON DELETE SET NULL,
    due_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ticket_comments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    body TEXT NOT NULL,
    internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tickets_tenant_status ON tickets(tenant_id, status);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id);
