-- ============================================================================
-- SmartStock — Inventory Management System
-- Script: 04_sample_data.sql
-- Description: Realistic seed data for Categories, Suppliers, Products, and Movements
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. SEED CATEGORIES
-- ----------------------------------------------------------------------------
INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES ('Electronics', 'Consumer electronic devices, displays, and audio gadgets');
INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES ('Stationery', 'Paper goods, pens, notebooks, and office essentials');
INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES ('Furniture', 'Desks, ergonomic chairs, and office workstation furniture');
INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES ('Computer Accessories', 'Keyboards, mice, hubs, docking stations, and peripherals');
INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES ('Networking', 'Cables, routers, switches, and network adapters');

-- ----------------------------------------------------------------------------
-- 2. SEED SUPPLIERS
-- ----------------------------------------------------------------------------
INSERT INTO SUPPLIERS (SUPPLIER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Apex Tech Distribution', '+1-555-0101', 'orders@apextech.com', '100 Innovation Way, Suite 400, San Jose, CA 95134');

INSERT INTO SUPPLIERS (SUPPLIER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Global Office Supply Co.', '+1-555-0102', 'sales@globalofficesup.com', '45 Paper Lane, Warehouse District, Chicago, IL 60607');

INSERT INTO SUPPLIERS (SUPPLIER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Prime Comfort Ergonomics', '+1-555-0103', 'support@primecomfort.com', '789 Spine Blvd, Grand Rapids, MI 49503');

INSERT INTO SUPPLIERS (SUPPLIER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('ByteCraft Peripherals', '+1-555-0104', 'supply@bytecraft.io', '22 Silicon Alley, Austin, TX 78701');

-- ----------------------------------------------------------------------------
-- 3. SEED PRODUCTS
-- ----------------------------------------------------------------------------
-- Product 1: Mechanical Gaming Keyboard (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('KB-MECH-01', 'RGB Mechanical Gaming Keyboard (Brown Switch)', 4, 4, 45.00, 89.99, 35, 10, 'ACTIVE');

-- Product 2: Wireless Ergonomic Mouse (LOW STOCK ALERT: qty 4 <= min 10)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('MS-WRLS-02', 'Wireless Precision Ergonomic Mouse', 4, 4, 18.50, 39.99, 4, 10, 'ACTIVE');

-- Product 3: 27-inch 4K IPS Monitor (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('MON-4K-27', '27-inch Ultra-HD 4K IPS Monitor', 1, 1, 180.00, 299.99, 14, 5, 'ACTIVE');

-- Product 4: Ergonomic Mesh High-Back Chair (OUT OF STOCK: qty 0)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('CHR-MESH-01', 'Ergonomic Mesh High-Back Executive Chair', 3, 3, 110.00, 219.00, 0, 4, 'ACTIVE');

-- Product 5: Executive Hardcover Notebook A5 (High stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('NTB-EXEC-A5', 'Executive Hardcover Journal Notebook A5 (192 Pages)', 2, 2, 3.50, 9.99, 140, 25, 'ACTIVE');

-- Product 6: Premium Gel Ink Pen Box (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('PEN-GEL-12', 'Fine Tip Black Gel Ink Pen (Pack of 12)', 2, 2, 4.20, 12.50, 58, 20, 'ACTIVE');

-- Product 7: Cat6 Ethernet Cable 10m (LOW STOCK ALERT: qty 3 <= min 15)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('CBL-CAT6-10M', 'High-Speed Shielded Cat6 Ethernet Cable (10m)', 5, 1, 2.10, 7.99, 3, 15, 'ACTIVE');

-- Product 8: USB-C 7-in-1 Multiport Adapter (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('HUB-USBC-7IN1', 'USB-C 7-in-1 Aluminium Hub with 4K HDMI', 4, 4, 14.00, 34.99, 22, 8, 'ACTIVE');

-- Product 9: Standing Desk Converter (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('DSK-STAND-01', 'Dual-Tier Gas-Spring Standing Desk Riser', 3, 3, 85.00, 159.00, 8, 5, 'ACTIVE');

-- Product 10: Active Noise-Cancelling Headset (Normal stock)
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, CATEGORY_ID, SUPPLIER_ID, PURCHASE_PRICE, SELLING_PRICE, QUANTITY, MIN_STOCK_LEVEL, STATUS)
VALUES ('AUD-NC-HEADSET', 'Over-Ear Active Noise-Cancelling Wireless Headset', 1, 1, 52.00, 109.99, 19, 6, 'ACTIVE');

-- ----------------------------------------------------------------------------
-- 4. SEED STOCK MOVEMENTS (Auditable History)
-- ----------------------------------------------------------------------------
-- KB-MECH-01 initial and movements
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (1, 'STOCK_IN', 50, 0, 50, 'PO-2026-1001', 'Initial bulk procurement from ByteCraft', CURRENT_TIMESTAMP - INTERVAL '10' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (1, 'STOCK_OUT', 15, 50, 35, 'SO-2026-0512', 'Corporate office equipment dispatch', CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- MS-WRLS-02
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (2, 'STOCK_IN', 20, 0, 20, 'PO-2026-1002', 'Opening inventory receiving', CURRENT_TIMESTAMP - INTERVAL '8' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (2, 'STOCK_OUT', 16, 20, 4, 'SO-2026-0520', 'Bulk workstation accessories fulfillment', CURRENT_TIMESTAMP - INTERVAL '1' DAY);

-- MON-4K-27
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (3, 'STOCK_IN', 20, 0, 20, 'PO-2026-1003', 'Initial shipment from Apex Tech', CURRENT_TIMESTAMP - INTERVAL '12' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (3, 'STOCK_OUT', 6, 20, 14, 'SO-2026-0524', 'Creative lab deployment', CURRENT_TIMESTAMP - INTERVAL '4' DAY);

-- CHR-MESH-01 (Depleted to 0)
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (4, 'STOCK_IN', 10, 0, 10, 'PO-2026-1004', 'Ergonomic seating consignment', CURRENT_TIMESTAMP - INTERVAL '15' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (4, 'STOCK_OUT', 10, 10, 0, 'SO-2026-0530', 'Executive suites furniture renovation (Stock depleted)', CURRENT_TIMESTAMP - INTERVAL '2' DAY);

-- NTB-EXEC-A5
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (5, 'STOCK_IN', 150, 0, 150, 'PO-2026-1005', 'Semester start stationery restocking', CURRENT_TIMESTAMP - INTERVAL '6' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (5, 'STOCK_OUT', 10, 150, 140, 'SO-2026-0535', 'Departmental starter kits', CURRENT_TIMESTAMP - INTERVAL '1' DAY);

-- CBL-CAT6-10M
INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (7, 'STOCK_IN', 25, 0, 25, 'PO-2026-1006', 'Network cabling delivery', CURRENT_TIMESTAMP - INTERVAL '9' DAY);

INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS, CREATED_AT)
VALUES (7, 'STOCK_OUT', 22, 25, 3, 'SO-2026-0540', 'Server room rewiring project', CURRENT_TIMESTAMP - INTERVAL '5' HOUR);

COMMIT;
