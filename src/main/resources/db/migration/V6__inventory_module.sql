-- Flyway Migration V6: Inventory Module
-- Adds SKU-based inventory, reservations, and movement ledger.

CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    unit_price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    on_hand_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_items_sku UNIQUE (sku),
    CONSTRAINT chk_inventory_on_hand_non_negative CHECK (on_hand_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_not_over_on_hand CHECK (reserved_quantity <= on_hand_quantity)
);

CREATE TABLE inventory_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL REFERENCES inventory_items(sku),
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    release_reason VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_reservations_order_sku UNIQUE (order_id, sku),
    CONSTRAINT chk_inventory_reservation_quantity_positive CHECK (quantity > 0)
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(120) NOT NULL REFERENCES inventory_items(sku),
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    payment_id BIGINT REFERENCES payments(id) ON DELETE SET NULL,
    reason VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE order_items
    ADD COLUMN sku VARCHAR(120);

UPDATE order_items
SET sku = 'LEGACY-' || id
WHERE sku IS NULL;

ALTER TABLE order_items
    ALTER COLUMN sku SET NOT NULL;

CREATE INDEX idx_inventory_items_sku ON inventory_items(sku);
CREATE INDEX idx_inventory_items_active ON inventory_items(active);
CREATE INDEX idx_inventory_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_inventory_reservations_sku_status ON inventory_reservations(sku, status);
CREATE INDEX idx_inventory_movements_sku ON inventory_movements(sku);
CREATE INDEX idx_inventory_movements_created_date ON inventory_movements(created_date DESC);
CREATE INDEX idx_order_items_sku ON order_items(sku);
