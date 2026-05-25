-- Flyway Migration V7: Multi-Tenancy Layer

CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenant_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    timezone VARCHAR(64) DEFAULT 'UTC',
    currency VARCHAR(3) DEFAULT 'USD',
    logo_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenant_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_code VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE (tenant_id, product_code)
);

CREATE TABLE tenant_users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    tenant_role VARCHAR(30) NOT NULL DEFAULT 'STAFF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_tenant_users_user_id ON tenant_users(user_id);
CREATE INDEX idx_tenant_subscriptions_tenant_id ON tenant_subscriptions(tenant_id);

-- Default tenant for existing data
INSERT INTO tenants (name, slug, status) VALUES ('Default Organization', 'default', 'ACTIVE');

INSERT INTO tenant_settings (tenant_id) VALUES (1);

INSERT INTO tenant_subscriptions (tenant_id, product_code, active) VALUES
    (1, 'ORDER_MANAGEMENT', TRUE),
    (1, 'INVENTORY_MANAGEMENT', TRUE),
    (1, 'CRM', TRUE),
    (1, 'TICKETING', TRUE),
    (1, 'PAYROLL', TRUE),
    (1, 'WORKFORCE', TRUE);

-- Add tenant_id to commerce tables
ALTER TABLE orders ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE orders SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE orders ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);

ALTER TABLE inventory_items ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE inventory_items SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE inventory_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE inventory_items DROP CONSTRAINT IF EXISTS uk_inventory_items_sku CASCADE;
ALTER TABLE inventory_items ADD CONSTRAINT uk_inventory_items_tenant_sku UNIQUE (tenant_id, sku);
CREATE INDEX idx_inventory_items_tenant_id ON inventory_items(tenant_id);

-- Link existing users to default tenant
INSERT INTO tenant_users (tenant_id, user_id, tenant_role, active)
SELECT 1, id, 'OWNER', TRUE FROM app_user
ON CONFLICT (tenant_id, user_id) DO NOTHING;
