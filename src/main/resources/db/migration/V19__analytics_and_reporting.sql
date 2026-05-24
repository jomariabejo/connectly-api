-- Flyway Migration V19: Analytics and Reporting
-- Implements comprehensive analytics, reports, dashboards, and profit estimation

-- 1. Create sales_analytics table - Aggregated sales data for reporting
CREATE TABLE IF NOT EXISTS sales_analytics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    total_sales DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_units_sold DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_transactions INT NOT NULL DEFAULT 0,
    avg_transaction_value DECIMAL(15, 2),
    top_product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    top_category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, analytics_date, period_type),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 2. Create profit_analytics table - Profit margin calculations per product/category
CREATE TABLE IF NOT EXISTS profit_analytics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    analytics_date DATE NOT NULL,
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    total_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_cost DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    profit_margin_percent DECIMAL(5, 2), -- Percentage: 0-100
    units_sold DECIMAL(12, 2) NOT NULL DEFAULT 0,
    avg_cost_per_unit DECIMAL(15, 2),
    avg_selling_price DECIMAL(15, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, product_id, category_id, analytics_date, period_type),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 3. Create dashboard_metrics table - Cached dashboard KPIs for performance
CREATE TABLE IF NOT EXISTS dashboard_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    total_sales_mtd DECIMAL(15, 2) NOT NULL DEFAULT 0, -- Month to date
    total_sales_ytd DECIMAL(15, 2) NOT NULL DEFAULT 0, -- Year to date
    gross_profit_mtd DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_profit_ytd DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_customers INT NOT NULL DEFAULT 0,
    inventory_value DECIMAL(15, 2) NOT NULL DEFAULT 0, -- Total value of current stock
    low_stock_count INT NOT NULL DEFAULT 0, -- Count of low stock items
    top_selling_product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    avg_profit_margin_percent DECIMAL(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, metric_date),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 4. Create inventory_metrics table - Inventory turnover and movement analytics
CREATE TABLE IF NOT EXISTS inventory_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    beginning_quantity DECIMAL(12, 2) NOT NULL,
    ending_quantity DECIMAL(12, 2) NOT NULL,
    units_sold DECIMAL(12, 2) NOT NULL DEFAULT 0,
    units_received DECIMAL(12, 2) NOT NULL DEFAULT 0,
    avg_inventory DECIMAL(12, 2),
    inventory_turnover_ratio DECIMAL(8, 2), -- Times inventory turned over in period
    days_inventory_outstanding INT, -- Average days inventory held
    stock_out_incidents INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, product_id, analytics_date, period_type),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 5. Create customer_analytics table - Customer purchasing patterns
CREATE TABLE IF NOT EXISTS customer_analytics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    total_purchases DECIMAL(15, 2) NOT NULL DEFAULT 0,
    purchase_count INT NOT NULL DEFAULT 0,
    avg_purchase_value DECIMAL(15, 2),
    last_purchase_date DATE,
    days_since_last_purchase INT,
    total_units_purchased DECIMAL(12, 2) NOT NULL DEFAULT 0,
    preferred_category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    customer_lifetime_value DECIMAL(15, 2), -- Running total
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, customer_id, analytics_date, period_type),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 6. Create profit_estimations table - Forecasted profit data
CREATE TABLE IF NOT EXISTS profit_estimations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    estimation_date DATE NOT NULL,
    forecast_start_date DATE NOT NULL,
    forecast_end_date DATE NOT NULL,
    forecast_period VARCHAR(20) NOT NULL, -- WEEKLY, MONTHLY, QUARTERLY, YEARLY
    estimated_total_revenue DECIMAL(15, 2) NOT NULL,
    estimated_total_cost DECIMAL(15, 2) NOT NULL,
    estimated_gross_profit DECIMAL(15, 2) NOT NULL,
    estimated_profit_margin_percent DECIMAL(5, 2),
    confidence_level VARCHAR(20), -- LOW, MEDIUM, HIGH
    methodology VARCHAR(100), -- LINEAR_TREND, SEASONAL_ADJUSTED, MOVING_AVERAGE
    based_on_historical_periods INT, -- Number of past periods used for forecast
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, forecast_start_date, forecast_end_date, forecast_period),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 7. Create category_analytics table - Analytics by product category
CREATE TABLE IF NOT EXISTS category_analytics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    total_sales DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_units_sold DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_transactions INT NOT NULL DEFAULT 0,
    gross_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    profit_margin_percent DECIMAL(5, 2),
    avg_product_price DECIMAL(15, 2),
    product_count INT, -- Number of products in category
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, category_id, analytics_date, period_type),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_sales_analytics_tenant_date ON sales_analytics(tenant_id, analytics_date DESC);
CREATE INDEX idx_sales_analytics_period ON sales_analytics(tenant_id, period_type, analytics_date DESC);
CREATE INDEX idx_profit_analytics_tenant_product_date ON profit_analytics(tenant_id, product_id, analytics_date DESC);
CREATE INDEX idx_profit_analytics_period ON profit_analytics(tenant_id, period_type, analytics_date DESC);
CREATE INDEX idx_dashboard_metrics_tenant_date ON dashboard_metrics(tenant_id, metric_date DESC);
CREATE INDEX idx_inventory_metrics_tenant_product_date ON inventory_metrics(tenant_id, product_id, analytics_date DESC);
CREATE INDEX idx_customer_analytics_tenant_customer_date ON customer_analytics(tenant_id, customer_id, analytics_date DESC);
CREATE INDEX idx_profit_estimations_tenant_forecast ON profit_estimations(tenant_id, forecast_start_date DESC);
CREATE INDEX idx_category_analytics_tenant_date ON category_analytics(tenant_id, analytics_date DESC);

-- Add audit logging columns
ALTER TABLE sales_analytics ADD COLUMN audit_user_id BIGINT;
ALTER TABLE profit_analytics ADD COLUMN audit_user_id BIGINT;
ALTER TABLE dashboard_metrics ADD COLUMN audit_user_id BIGINT;
ALTER TABLE inventory_metrics ADD COLUMN audit_user_id BIGINT;
ALTER TABLE customer_analytics ADD COLUMN audit_user_id BIGINT;
ALTER TABLE profit_estimations ADD COLUMN audit_user_id BIGINT;
ALTER TABLE category_analytics ADD COLUMN audit_user_id BIGINT;
