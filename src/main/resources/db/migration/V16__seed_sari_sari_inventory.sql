-- Flyway Migration V16: Seed Sari-Sari inventory catalog for default tenant

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'NOOD-001', 'Lucky Me Pancit Canton Original', 12.00, 'PHP', 320, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'NOOD-002', 'Lucky Me Pancit Canton Chilimansi', 12.00, 'PHP', 235, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'NOOD-003', 'Lucky Me Beef Instant Mami', 13.00, 'PHP', 125, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'NOOD-004', 'Nissin Cup Noodles Seafood', 35.00, 'PHP', 78, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CAN-001', 'Argentina Corned Beef 260g', 115.00, 'PHP', 48, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CAN-002', '555 Sardines Tausi', 28.00, 'PHP', 128, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CAN-003', 'Century Tuna Flakes in Oil', 40.00, 'PHP', 87, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BEV-001', 'Coca-Cola 1.5L', 65.00, 'PHP', 50, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BEV-002', 'Cobra Energy Drink 350ml', 35.00, 'PHP', 70, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BEV-003', 'Zesto Orange 200ml', 8.00, 'PHP', 126, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BEV-004', 'Wilkins Distilled 7L', 70.00, 'PHP', 24, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SNK-001', 'Oishi Prawn Crackers', 10.00, 'PHP', 155, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SNK-002', 'Piattos Cheese', 22.00, 'PHP', 90, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SNK-003', 'Chippy Barbecue', 12.00, 'PHP', 24, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'RICE-001', 'Sinandomeng Rice 1kg', 50.00, 'PHP', 115, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'RICE-002', 'Jasmine Rice 5kg', 265.00, 'PHP', 42, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'COND-001', 'Mama Sita Sinigang Mix', 22.00, 'PHP', 65, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'COND-002', 'Silver Swan Soy Sauce 1L', 48.00, 'PHP', 37, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'COND-003', 'UFC Banana Ketchup 320g', 40.00, 'PHP', 50, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'TOIL-001', 'Safeguard White 135g', 48.00, 'PHP', 56, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'TOIL-002', 'Colgate Max Fresh 150g', 68.00, 'PHP', 39, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'TOIL-003', 'Surf Powder Rose Fresh 65g', 10.00, 'PHP', 290, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'TOIL-004', 'Joy Dishwashing Lemon 250ml', 35.00, 'PHP', 51, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SCH-001', 'Pilot Ballpen Blue', 15.00, 'PHP', 78, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SCH-002', 'Mongol Pencil #2', 7.00, 'PHP', 120, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SCH-003', 'Intermediate Pad', 25.00, 'PHP', 45, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'LOAD-001', 'Smart Load 50', 50.00, 'PHP', 25, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'LOAD-002', 'Globe Load 100', 100.00, 'PHP', 12, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'DAIRY-001', 'Alaska Evaporated 370ml', 35.00, 'PHP', 82, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'DAIRY-002', 'Bear Brand Powder 300g', 110.00, 'PHP', 29, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CANDY-001', 'Chocnut', 7.00, 'PHP', 230, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CANDY-002', 'Maxx Honey Lemon', 3.00, 'PHP', 450, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'EGG-001', 'Fresh Eggs', 9.00, 'PHP', 48, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'ICE-001', 'Ice Tubig 2kg', 20.00, 'PHP', 8, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CIG-001', 'Marlboro Red', 110.00, 'PHP', 115, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BAT-001', 'Energizer AA 2pcs', 55.00, 'PHP', 44, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'HOUSE-001', 'Champion Mosquito Coil', 10.00, 'PHP', 122, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'HOUSE-002', 'Zonrox Bleach 500ml', 28.00, 'PHP', 55, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BREAD-001', 'Tasty Bread', 45.00, 'PHP', 6, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'MILO-001', 'Milo Sachet 22g', 12.00, 'PHP', 175, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'COFF-001', 'Nescafe Classic 2g', 5.00, 'PHP', 350, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'CAN-004', 'Purefoods Corned Beef 380g', 175.00, 'PHP', 21, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'NOOD-005', 'Indomie Fried Noodles', 15.00, 'PHP', 95, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'BEV-005', 'Royal Tru-Orange 1.5L', 60.00, 'PHP', 42, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'SNK-004', 'Nova Country Cheddar', 15.00, 'PHP', 113, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'COND-004', 'Ajinomoto 8g', 4.00, 'PHP', 420, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'TOIL-005', 'Palmolive Shampoo Sachet', 5.00, 'PHP', 295, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'ELEC-001', 'Fujitsu AA Batteries 4pcs', 110.00, 'PHP', 19, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
VALUES (1, 'ELEC-002', 'Extension Cord 3m', 250.00, 'PHP', 10, 0, TRUE)
ON CONFLICT (tenant_id, sku) DO NOTHING;

-- Mirror catalog to Aling Maria store (tenant 2) when present
INSERT INTO inventory_items (tenant_id, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active)
SELECT 2, sku, name, unit_price, currency, on_hand_quantity, reserved_quantity, active
FROM inventory_items
WHERE tenant_id = 1
ON CONFLICT (tenant_id, sku) DO NOTHING;

