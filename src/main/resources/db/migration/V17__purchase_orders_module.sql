-- Flyway Migration V17: Purchase Orders Module

CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    po_number VARCHAR(40) NOT NULL,
    supplier_name VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    notes TEXT,
    expected_delivery_date DATE,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_by_user_id BIGINT NOT NULL REFERENCES app_user(id),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_purchase_orders_tenant_po_number UNIQUE (tenant_id, po_number),
    CONSTRAINT chk_purchase_orders_total_non_negative CHECK (total_amount >= 0)
);

CREATE TABLE purchase_order_lines (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity_ordered INTEGER NOT NULL,
    quantity_received INTEGER NOT NULL DEFAULT 0,
    unit_cost DECIMAL(15,2) NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,
    CONSTRAINT chk_po_line_quantity_ordered_positive CHECK (quantity_ordered > 0),
    CONSTRAINT chk_po_line_quantity_received_non_negative CHECK (quantity_received >= 0),
    CONSTRAINT chk_po_line_quantity_received_not_over_ordered CHECK (quantity_received <= quantity_ordered),
    CONSTRAINT chk_po_line_unit_cost_non_negative CHECK (unit_cost >= 0),
    CONSTRAINT chk_po_line_total_non_negative CHECK (line_total >= 0)
);

CREATE INDEX idx_purchase_orders_tenant_id ON purchase_orders(tenant_id);
CREATE INDEX idx_purchase_orders_tenant_status ON purchase_orders(tenant_id, status);
CREATE INDEX idx_purchase_orders_tenant_created ON purchase_orders(tenant_id, created_date DESC);
CREATE INDEX idx_purchase_order_lines_po_id ON purchase_order_lines(purchase_order_id);

ALTER TABLE inventory_movements
    ADD COLUMN purchase_order_id BIGINT REFERENCES purchase_orders(id) ON DELETE SET NULL;

CREATE INDEX idx_inventory_movements_purchase_order_id ON inventory_movements(purchase_order_id);
