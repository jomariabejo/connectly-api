-- Flyway Migration V4: Orders Module
-- Creates tables for Orders, OrderItems, and OrderStatusHistory with optimized indexes

-- Create orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    marketplace_source VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create order_items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL
);

-- Create order_status_history table (audit trail)
CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

-- Create indexes for query performance (CRITICAL for filtering/sorting)

-- Single column indexes for common filters
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_marketplace ON orders(marketplace_source);
CREATE INDEX idx_orders_created_date ON orders(created_date DESC);
CREATE INDEX idx_orders_updated_date ON orders(updated_date DESC);

-- Composite indexes for combined filtering (highly optimized)
CREATE INDEX idx_orders_status_date ON orders(status, created_date DESC);
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_orders_customer_date ON orders(customer_id, created_date DESC);

-- Indexes for order_items (prevent N+1 queries)
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Indexes for order_status_history (audit trail queries)
CREATE INDEX idx_status_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_status_history_changed_at ON order_status_history(changed_at DESC);
CREATE INDEX idx_status_history_order_date ON order_status_history(order_id, changed_at DESC);
