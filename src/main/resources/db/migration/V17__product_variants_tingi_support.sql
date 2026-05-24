-- Flyway Migration V17: Product Variants & Tingi Support
-- Implements Filipino "tingi" culture - selling individual units from multipacks
-- Example: 1L bottle of Coke sold per glass, or pack of 10 sold individually

-- 1. Create product_variants table - hierarchical product support
CREATE TABLE IF NOT EXISTS product_variants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    parent_product_id BIGINT NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit_type VARCHAR(50) NOT NULL, -- PIECES, GLASS, PACK, BOTTLE, etc.
    quantity_per_unit DECIMAL(10, 2) NOT NULL, -- 1 glass = 0.2L, 1 piece = 1 piece
    retail_price DECIMAL(15, 2) NOT NULL,
    wholesale_price DECIMAL(15, 2),
    on_hand_quantity DECIMAL(10, 2) DEFAULT 0,
    reserved_quantity DECIMAL(10, 2) DEFAULT 0,
    reorder_level DECIMAL(10, 2),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DISCONTINUED
    image_key VARCHAR(255), -- s3 key for product image
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, sku),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 2. Create variant_pricing table - bulk discount pricing
CREATE TABLE IF NOT EXISTS variant_pricing (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity_threshold DECIMAL(10, 2) NOT NULL, -- minimum quantity for this price
    price DECIMAL(15, 2) NOT NULL,
    price_type VARCHAR(20) DEFAULT 'RETAIL', -- RETAIL, WHOLESALE, BULK
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(variant_id, quantity_threshold)
);

-- 3. Create tingi_sales table - track tingi sales separately for analytics
CREATE TABLE IF NOT EXISTS tingi_sales (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity_sold DECIMAL(10, 2) NOT NULL,
    price_per_unit DECIMAL(15, 2) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 4. Modify inventory_items to support hierarchical products
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS parent_product_id BIGINT;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS unit_type VARCHAR(50) DEFAULT 'PACK';
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS image_key VARCHAR(255);
ALTER TABLE inventory_items ADD CONSTRAINT fk_inventory_items_parent_product FOREIGN KEY (parent_product_id) REFERENCES inventory_items(id) ON DELETE SET NULL;

-- 5. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_product_variants_tenant_sku ON product_variants(tenant_id, sku);
CREATE INDEX IF NOT EXISTS idx_product_variants_parent_product ON product_variants(tenant_id, parent_product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_status ON product_variants(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_variant_pricing_variant_id ON variant_pricing(variant_id);
CREATE INDEX IF NOT EXISTS idx_variant_pricing_quantity ON variant_pricing(quantity_threshold DESC);

CREATE INDEX IF NOT EXISTS idx_tingi_sales_tenant_order ON tingi_sales(tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_tingi_sales_variant_id ON tingi_sales(variant_id);
CREATE INDEX IF NOT EXISTS idx_tingi_sales_created_at ON tingi_sales(tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_items_parent_product ON inventory_items(parent_product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_unit_type ON inventory_items(tenant_id, unit_type);
