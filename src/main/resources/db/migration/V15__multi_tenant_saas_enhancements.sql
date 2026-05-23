-- Flyway Migration V15: Multi-Tenant SaaS Architecture Enhancements

-- 1. Extend tenants table with new fields for SaaS features
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS business_type VARCHAR(50) DEFAULT 'GENERAL';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS subdomain VARCHAR(100) UNIQUE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS pricing_tier VARCHAR(50) DEFAULT 'STARTER';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMP;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS subscription_active BOOLEAN DEFAULT TRUE;

-- Create index on subdomain for efficient tenant resolution
CREATE INDEX IF NOT EXISTS idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX IF NOT EXISTS idx_tenants_business_type ON tenants(business_type);
CREATE INDEX IF NOT EXISTS idx_tenants_pricing_tier ON tenants(pricing_tier);

-- 2. Create pricing_plans table
CREATE TABLE IF NOT EXISTS pricing_plans (
    id BIGSERIAL PRIMARY KEY,
    tier VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price_php DECIMAL(10, 2) NOT NULL,
    price_usd DECIMAL(10, 2) NOT NULL,
    max_users INT,
    max_locations INT,
    max_storage_gb INT,
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert Philippine market pricing tiers
INSERT INTO pricing_plans (tier, name, description, price_php, price_usd, max_users, max_locations, max_storage_gb, active)
VALUES
    ('STARTER', 'Starter Plan', 'Perfect for individual sari-sari stores and small businesses', 499, 9, 5, 1, 1, TRUE),
    ('PROFESSIONAL', 'Professional Plan', 'Ideal for growing businesses with multiple locations', 1499, 27, -1, 3, 10, TRUE),
    ('BUSINESS', 'Business Plan', 'For established businesses needing unlimited features', 3999, 72, -1, -1, 50, TRUE),
    ('ENTERPRISE', 'Enterprise Plan', 'Custom solutions for large organizations', 0, 0, -1, -1, -1, TRUE)
ON CONFLICT (tier) DO NOTHING;

-- 3. Create feature_access table for tier-based feature flags
CREATE TABLE IF NOT EXISTS feature_access (
    id BIGSERIAL PRIMARY KEY,
    pricing_tier VARCHAR(50) NOT NULL,
    feature_code VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(pricing_tier, feature_code),
    FOREIGN KEY (pricing_tier) REFERENCES pricing_plans(tier) ON DELETE CASCADE
);

-- Insert feature access control for each tier
-- STARTER tier features
INSERT INTO feature_access (pricing_tier, feature_code, enabled)
VALUES
    ('STARTER', 'BASIC_INVENTORY', TRUE),
    ('STARTER', 'BASIC_SALES', TRUE),
    ('STARTER', 'BASIC_REPORTS', TRUE),
    ('STARTER', 'EMAIL_SUPPORT', TRUE),
    ('STARTER', 'CREDIT_TRACKING', FALSE),
    ('STARTER', 'ADVANCED_ANALYTICS', FALSE),
    ('STARTER', 'API_ACCESS', FALSE),
    ('STARTER', 'CUSTOM_INTEGRATIONS', FALSE)
ON CONFLICT (pricing_tier, feature_code) DO NOTHING;

-- PROFESSIONAL tier features
INSERT INTO feature_access (pricing_tier, feature_code, enabled)
VALUES
    ('PROFESSIONAL', 'BASIC_INVENTORY', TRUE),
    ('PROFESSIONAL', 'BASIC_SALES', TRUE),
    ('PROFESSIONAL', 'BASIC_REPORTS', TRUE),
    ('PROFESSIONAL', 'EMAIL_SUPPORT', TRUE),
    ('PROFESSIONAL', 'CREDIT_TRACKING', TRUE),
    ('PROFESSIONAL', 'ADVANCED_ANALYTICS', TRUE),
    ('PROFESSIONAL', 'API_ACCESS', FALSE),
    ('PROFESSIONAL', 'CUSTOM_INTEGRATIONS', FALSE)
ON CONFLICT (pricing_tier, feature_code) DO NOTHING;

-- BUSINESS tier features
INSERT INTO feature_access (pricing_tier, feature_code, enabled)
VALUES
    ('BUSINESS', 'BASIC_INVENTORY', TRUE),
    ('BUSINESS', 'BASIC_SALES', TRUE),
    ('BUSINESS', 'BASIC_REPORTS', TRUE),
    ('BUSINESS', 'EMAIL_SUPPORT', TRUE),
    ('BUSINESS', 'CREDIT_TRACKING', TRUE),
    ('BUSINESS', 'ADVANCED_ANALYTICS', TRUE),
    ('BUSINESS', 'API_ACCESS', TRUE),
    ('BUSINESS', 'CUSTOM_INTEGRATIONS', TRUE)
ON CONFLICT (pricing_tier, feature_code) DO NOTHING;

-- ENTERPRISE tier features
INSERT INTO feature_access (pricing_tier, feature_code, enabled)
VALUES
    ('ENTERPRISE', 'BASIC_INVENTORY', TRUE),
    ('ENTERPRISE', 'BASIC_SALES', TRUE),
    ('ENTERPRISE', 'BASIC_REPORTS', TRUE),
    ('ENTERPRISE', 'EMAIL_SUPPORT', TRUE),
    ('ENTERPRISE', 'CREDIT_TRACKING', TRUE),
    ('ENTERPRISE', 'ADVANCED_ANALYTICS', TRUE),
    ('ENTERPRISE', 'API_ACCESS', TRUE),
    ('ENTERPRISE', 'CUSTOM_INTEGRATIONS', TRUE)
ON CONFLICT (pricing_tier, feature_code) DO NOTHING;

-- 4. Create usage_tracking table for resource quotas
CREATE TABLE IF NOT EXISTS usage_tracking (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tracking_month DATE NOT NULL,
    api_calls_used BIGINT DEFAULT 0,
    storage_used_gb BIGINT DEFAULT 0,
    sms_sent INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, tracking_month),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_usage_tracking_tenant_id ON usage_tracking(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_tracking_month ON usage_tracking(tracking_month);

-- 5. Create tenant_audit_log table for security and compliance
CREATE TABLE IF NOT EXISTS tenant_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id ON tenant_audit_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON tenant_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON tenant_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource ON tenant_audit_log(resource_type, resource_id);

-- 6. Create tenant_invitations_log table
CREATE TABLE IF NOT EXISTS tenant_invitations_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    invited_by BIGINT,
    accepted_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tenant_invitations_log_tenant ON tenant_invitations_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_invitations_log_email ON tenant_invitations_log(email);
CREATE INDEX IF NOT EXISTS idx_tenant_invitations_log_status ON tenant_invitations_log(status);

-- 7. Extend tenant_settings table with more configuration options
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS business_type_config JSONB DEFAULT '{}'::jsonb;
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'en';

-- 8. Create business type templates table
CREATE TABLE IF NOT EXISTS business_type_templates (
    id BIGSERIAL PRIMARY KEY,
    business_type VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(512),
    default_settings JSONB,
    sample_data_sql TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert business type templates
INSERT INTO business_type_templates (business_type, display_name, description, default_settings)
VALUES
    ('SARI_SARI_STORE', 'Sari-Sari Store', 'Small convenience store for inventory and credit tracking', '{"features": ["inventory", "credit_tracking", "customer_ledger"]}'),
    ('VULCANIZING_SHOP', 'Vulcanizing Shop', 'Tire and repair shop management', '{"features": ["service_tracking", "parts_inventory", "customer_vehicles"]}'),
    ('CATERING_BUSINESS', 'Catering Business', 'Event and menu management', '{"features": ["event_management", "menu_planning", "order_management", "costing"]}'),
    ('GENERAL', 'General Business', 'Standard business management tools', '{"features": ["inventory", "sales", "basic_reporting"]}')
ON CONFLICT (business_type) DO NOTHING;

-- 9. Create index for better tenant context resolution
ALTER TABLE tenant_users ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_tenant_users_tenant_user_active ON tenant_users(tenant_id, user_id, active);

-- Update default tenant with STARTER tier if not already set
UPDATE tenants SET 
    business_type = 'GENERAL',
    subdomain = 'default',
    pricing_tier = 'STARTER'
WHERE id = 1 AND subdomain IS NULL;
