-- Flyway Migration V8: Customer Management (CRM)

CREATE TABLE crm_customers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255),
    phone VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    company VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE crm_customer_tags (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    UNIQUE (tenant_id, name)
);

CREATE TABLE crm_customer_tag_map (
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES crm_customer_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (customer_id, tag_id)
);

CREATE TABLE crm_customer_notes (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE crm_customer_interactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES crm_customers(id) ON DELETE CASCADE,
    interaction_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    description TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_crm_customers_tenant ON crm_customers(tenant_id);
CREATE INDEX idx_crm_customers_email ON crm_customers(tenant_id, email);
