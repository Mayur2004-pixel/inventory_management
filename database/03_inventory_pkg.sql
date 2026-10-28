-- ============================================================================
-- SmartStock — Inventory Management System
-- Script: 03_inventory_pkg.sql
-- Description: Oracle PL/SQL Package Specification and Body for INVENTORY_PKG
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PACKAGE SPECIFICATION
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PACKAGE INVENTORY_PKG AS

    -- Add a new product with business validations
    PROCEDURE ADD_PRODUCT (
        p_sku            IN  VARCHAR2,
        p_name           IN  VARCHAR2,
        p_cat_id         IN  NUMBER,
        p_sup_id         IN  NUMBER,
        p_purchase_price IN  NUMBER,
        p_selling_price  IN  NUMBER,
        p_initial_qty    IN  NUMBER,
        p_min_stock      IN  NUMBER,
        p_status         IN  VARCHAR2,
        o_product_id     OUT NUMBER
    );

    -- Update existing product details
    PROCEDURE UPDATE_PRODUCT (
        p_product_id     IN  NUMBER,
        p_sku            IN  VARCHAR2,
        p_name           IN  VARCHAR2,
        p_cat_id         IN  NUMBER,
        p_sup_id         IN  NUMBER,
        p_purchase_price IN  NUMBER,
        p_selling_price  IN  NUMBER,
        p_min_stock      IN  NUMBER,
        p_status         IN  VARCHAR2
    );

    -- Receive incoming stock (atomic pessimistic locking & audit logging)
    PROCEDURE STOCK_IN (
        p_product_id     IN  NUMBER,
        p_supplier_id    IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_unit_price     IN  NUMBER,
        p_reference_no   IN  VARCHAR2,
        p_remarks        IN  VARCHAR2,
        o_new_quantity   OUT NUMBER
    );

    -- Issue / sell stock (pessimistic lock, prevents negative stock)
    PROCEDURE STOCK_OUT (
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_reference_no   IN  VARCHAR2,
        p_remarks        IN  VARCHAR2,
        o_new_quantity   OUT NUMBER
    );

    -- Fetch low-stock alert products via REF CURSOR
    PROCEDURE GET_LOW_STOCK (
        o_cursor         OUT SYS_REFCURSOR
    );

    -- Return available quantity for a product
    FUNCTION GET_STOCK_QUANTITY (
        p_product_id     IN  NUMBER
    ) RETURN NUMBER;

    -- Return total count of active products
    FUNCTION GET_TOTAL_PRODUCTS RETURN NUMBER;

END INVENTORY_PKG;
/

-- ----------------------------------------------------------------------------
-- PACKAGE BODY
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PACKAGE BODY INVENTORY_PKG AS

    -- ------------------------------------------------------------------------
    -- 1. ADD_PRODUCT
    -- ------------------------------------------------------------------------
    PROCEDURE ADD_PRODUCT (
        p_sku            IN  VARCHAR2,
        p_name           IN  VARCHAR2,
        p_cat_id         IN  NUMBER,
        p_sup_id         IN  NUMBER,
        p_purchase_price IN  NUMBER,
        p_selling_price  IN  NUMBER,
        p_initial_qty    IN  NUMBER,
        p_min_stock      IN  NUMBER,
        p_status         IN  VARCHAR2,
        o_product_id     OUT NUMBER
    ) IS
        v_sku_count   NUMBER := 0;
        v_cat_count   NUMBER := 0;
        v_sup_count   NUMBER := 0;
        v_qty         NUMBER := NVL(p_initial_qty, 0);
        v_min_stock   NUMBER := NVL(p_min_stock, 5);
        v_status      VARCHAR2(20) := NVL(UPPER(TRIM(p_status)), 'ACTIVE');
    BEGIN
        -- Rule 1 & 2: Name and SKU must not be blank
        IF p_sku IS NULL OR TRIM(p_sku) IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Product SKU is required and cannot be blank.');
        END IF;

        IF p_name IS NULL OR TRIM(p_name) IS NULL THEN
            RAISE_APPLICATION_ERROR(-20002, 'Product Name is required and cannot be blank.');
        END IF;

        -- Rule 1: SKU uniqueness check
        SELECT COUNT(*) INTO v_sku_count FROM PRODUCTS WHERE UPPER(SKU) = UPPER(TRIM(p_sku));
        IF v_sku_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'A product with SKU "' || TRIM(p_sku) || '" already exists.');
        END IF;

        -- Rule 3: Prices and quantities cannot be negative
        IF p_purchase_price < 0 THEN
            RAISE_APPLICATION_ERROR(-20004, 'Purchase price cannot be negative.');
        END IF;

        IF p_selling_price < 0 THEN
            RAISE_APPLICATION_ERROR(-20005, 'Selling price cannot be negative.');
        END IF;

        IF v_qty < 0 THEN
            RAISE_APPLICATION_ERROR(-20006, 'Initial quantity cannot be negative.');
        END IF;

        IF v_min_stock < 0 THEN
            RAISE_APPLICATION_ERROR(-20007, 'Minimum stock level cannot be negative.');
        END IF;

        -- Foreign key validation: Category
        SELECT COUNT(*) INTO v_cat_count FROM CATEGORIES WHERE CATEGORY_ID = p_cat_id;
        IF v_cat_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20008, 'Category ID ' || p_cat_id || ' does not exist.');
        END IF;

        -- Foreign key validation: Supplier (if provided)
        IF p_sup_id IS NOT NULL THEN
            SELECT COUNT(*) INTO v_sup_count FROM SUPPLIERS WHERE SUPPLIER_ID = p_sup_id;
            IF v_sup_count = 0 THEN
                RAISE_APPLICATION_ERROR(-20009, 'Supplier ID ' || p_sup_id || ' does not exist.');
            END IF;
        END IF;

        -- Insert product
        INSERT INTO PRODUCTS (
            SKU,
            PRODUCT_NAME,
            CATEGORY_ID,
            SUPPLIER_ID,
            PURCHASE_PRICE,
            SELLING_PRICE,
            QUANTITY,
            MIN_STOCK_LEVEL,
            STATUS
        ) VALUES (
            TRIM(p_sku),
            TRIM(p_name),
            p_cat_id,
            p_sup_id,
            p_purchase_price,
            p_selling_price,
            v_qty,
            v_min_stock,
            v_status
        ) RETURNING PRODUCT_ID INTO o_product_id;

        -- If initial stock is greater than 0, record initial stock movement
        IF v_qty > 0 THEN
            INSERT INTO STOCK_MOVEMENTS (
                PRODUCT_ID,
                MOVEMENT_TYPE,
                QUANTITY,
                PREVIOUS_QUANTITY,
                NEW_QUANTITY,
                REFERENCE_NUMBER,
                REMARKS
            ) VALUES (
                o_product_id,
                'STOCK_IN',
                v_qty,
                0,
                v_qty,
                'INIT-STOCK-' || o_product_id,
                'Initial stock registered upon product creation'
            );
        END IF;

    END ADD_PRODUCT;


    -- ------------------------------------------------------------------------
    -- 2. UPDATE_PRODUCT
    -- ------------------------------------------------------------------------
    PROCEDURE UPDATE_PRODUCT (
        p_product_id     IN  NUMBER,
        p_sku            IN  VARCHAR2,
        p_name           IN  VARCHAR2,
        p_cat_id         IN  NUMBER,
        p_sup_id         IN  NUMBER,
        p_purchase_price IN  NUMBER,
        p_selling_price  IN  NUMBER,
        p_min_stock      IN  NUMBER,
        p_status         IN  VARCHAR2
    ) IS
        v_exists NUMBER := 0;
        v_sku_count NUMBER := 0;
        v_cat_count NUMBER := 0;
        v_sup_count NUMBER := 0;
    BEGIN
        -- Check product exists
        SELECT COUNT(*) INTO v_exists FROM PRODUCTS WHERE PRODUCT_ID = p_product_id;
        IF v_exists = 0 THEN
            RAISE_APPLICATION_ERROR(-20020, 'Product ID ' || p_product_id || ' not found.');
        END IF;

        -- Validations
        IF p_sku IS NULL OR TRIM(p_sku) IS NULL THEN
            RAISE_APPLICATION_ERROR(-20021, 'Product SKU cannot be blank.');
        END IF;

        IF p_name IS NULL OR TRIM(p_name) IS NULL THEN
            RAISE_APPLICATION_ERROR(-20022, 'Product Name cannot be blank.');
        END IF;

        SELECT COUNT(*) INTO v_sku_count
        FROM PRODUCTS
        WHERE UPPER(SKU) = UPPER(TRIM(p_sku)) AND PRODUCT_ID != p_product_id;

        IF v_sku_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20023, 'Another product already uses SKU "' || TRIM(p_sku) || '".');
        END IF;

        IF p_purchase_price < 0 OR p_selling_price < 0 THEN
            RAISE_APPLICATION_ERROR(-20024, 'Prices cannot be negative.');
        END IF;

        IF p_min_stock < 0 THEN
            RAISE_APPLICATION_ERROR(-20025, 'Minimum stock cannot be negative.');
        END IF;

        SELECT COUNT(*) INTO v_cat_count FROM CATEGORIES WHERE CATEGORY_ID = p_cat_id;
        IF v_cat_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20026, 'Category ID ' || p_cat_id || ' does not exist.');
        END IF;

        IF p_sup_id IS NOT NULL THEN
            SELECT COUNT(*) INTO v_sup_count FROM SUPPLIERS WHERE SUPPLIER_ID = p_sup_id;
            IF v_sup_count = 0 THEN
                RAISE_APPLICATION_ERROR(-20027, 'Supplier ID ' || p_sup_id || ' does not exist.');
            END IF;
        END IF;

        UPDATE PRODUCTS
        SET SKU = TRIM(p_sku),
            PRODUCT_NAME = TRIM(p_name),
            CATEGORY_ID = p_cat_id,
            SUPPLIER_ID = p_sup_id,
            PURCHASE_PRICE = p_purchase_price,
            SELLING_PRICE = p_selling_price,
            MIN_STOCK_LEVEL = p_min_stock,
            STATUS = NVL(UPPER(TRIM(p_status)), STATUS)
        WHERE PRODUCT_ID = p_product_id;

    END UPDATE_PRODUCT;


    -- ------------------------------------------------------------------------
    -- 3. STOCK_IN (Receiving)
    -- ------------------------------------------------------------------------
    PROCEDURE STOCK_IN (
        p_product_id     IN  NUMBER,
        p_supplier_id    IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_unit_price     IN  NUMBER,
        p_reference_no   IN  VARCHAR2,
        p_remarks        IN  VARCHAR2,
        o_new_quantity   OUT NUMBER
    ) IS
        v_prev_qty   NUMBER;
        v_new_qty    NUMBER;
        v_ref_no     VARCHAR2(100);
        v_sup_count  NUMBER := 0;
    BEGIN
        -- Rule 4: Stock-in quantity must be greater than zero
        IF p_quantity IS NULL OR p_quantity <= 0 THEN
            RAISE_APPLICATION_ERROR(-20030, 'Stock-in quantity must be strictly greater than zero.');
        END IF;

        -- Validate supplier if provided
        IF p_supplier_id IS NOT NULL THEN
            SELECT COUNT(*) INTO v_sup_count FROM SUPPLIERS WHERE SUPPLIER_ID = p_supplier_id;
            IF v_sup_count = 0 THEN
                RAISE_APPLICATION_ERROR(-20031, 'Supplier ID ' || p_supplier_id || ' does not exist.');
            END IF;
        END IF;

        -- Pessimistic row locking on target product
        BEGIN
            SELECT QUANTITY
            INTO v_prev_qty
            FROM PRODUCTS
            WHERE PRODUCT_ID = p_product_id
            FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20032, 'Product ID ' || p_product_id || ' not found.');
        END;

        -- Compute new quantity
        v_new_qty := v_prev_qty + p_quantity;

        -- Generate reference number if omitted
        IF p_reference_no IS NULL OR TRIM(p_reference_no) IS NULL THEN
            v_ref_no := 'RCV-' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDD-HH24MISS');
        ELSE
            v_ref_no := TRIM(p_reference_no);
        END IF;

        -- Update product quantity and optionally supplier / purchase price
        UPDATE PRODUCTS
        SET QUANTITY = v_new_qty,
            PURCHASE_PRICE = CASE WHEN p_unit_price IS NOT NULL AND p_unit_price > 0 THEN p_unit_price ELSE PURCHASE_PRICE END,
            SUPPLIER_ID = NVL(p_supplier_id, SUPPLIER_ID)
        WHERE PRODUCT_ID = p_product_id;

        -- Record permanent stock movement
        INSERT INTO STOCK_MOVEMENTS (
            PRODUCT_ID,
            MOVEMENT_TYPE,
            QUANTITY,
            PREVIOUS_QUANTITY,
            NEW_QUANTITY,
            REFERENCE_NUMBER,
            REMARKS
        ) VALUES (
            p_product_id,
            'STOCK_IN',
            p_quantity,
            v_prev_qty,
            v_new_qty,
            v_ref_no,
            NVL(p_remarks, 'Stock received into inventory')
        );

        o_new_quantity := v_new_qty;

    END STOCK_IN;


    -- ------------------------------------------------------------------------
    -- 4. STOCK_OUT (Issue / Sales)
    -- ------------------------------------------------------------------------
    PROCEDURE STOCK_OUT (
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_reference_no   IN  VARCHAR2,
        p_remarks        IN  VARCHAR2,
        o_new_quantity   OUT NUMBER
    ) IS
        v_prev_qty   NUMBER;
        v_new_qty    NUMBER;
        v_ref_no     VARCHAR2(100);
        v_status     VARCHAR2(20);
    BEGIN
        -- Rule 5: Stock-out quantity must be greater than zero
        IF p_quantity IS NULL OR p_quantity <= 0 THEN
            RAISE_APPLICATION_ERROR(-20040, 'Stock-out quantity must be strictly greater than zero.');
        END IF;

        -- Pessimistic row locking on target product
        BEGIN
            SELECT QUANTITY, STATUS
            INTO v_prev_qty, v_status
            FROM PRODUCTS
            WHERE PRODUCT_ID = p_product_id
            FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20041, 'Product ID ' || p_product_id || ' not found.');
        END;

        -- Disallow issue if product is inactive
        IF UPPER(v_status) = 'INACTIVE' THEN
            RAISE_APPLICATION_ERROR(-20042, 'Cannot issue stock: Product is currently marked as INACTIVE.');
        END IF;

        -- Rule 6 & 10: Stock-out cannot exceed available stock (concurrency-safe)
        IF v_prev_qty < p_quantity THEN
            RAISE_APPLICATION_ERROR(
                -20043,
                'Insufficient stock available. Current balance is ' || v_prev_qty ||
                ', requested: ' || p_quantity || '. Issue rejected.'
            );
        END IF;

        v_new_qty := v_prev_qty - p_quantity;

        -- Generate reference number if omitted
        IF p_reference_no IS NULL OR TRIM(p_reference_no) IS NULL THEN
            v_ref_no := 'ISSUE-' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDD-HH24MISS');
        ELSE
            v_ref_no := TRIM(p_reference_no);
        END IF;

        -- Update product quantity
        UPDATE PRODUCTS
        SET QUANTITY = v_new_qty
        WHERE PRODUCT_ID = p_product_id;

        -- Record permanent stock movement
        INSERT INTO STOCK_MOVEMENTS (
            PRODUCT_ID,
            MOVEMENT_TYPE,
            QUANTITY,
            PREVIOUS_QUANTITY,
            NEW_QUANTITY,
            REFERENCE_NUMBER,
            REMARKS
        ) VALUES (
            p_product_id,
            'STOCK_OUT',
            p_quantity,
            v_prev_qty,
            v_new_qty,
            v_ref_no,
            NVL(p_remarks, 'Stock issued / sold from inventory')
        );

        o_new_quantity := v_new_qty;

    END STOCK_OUT;


    -- ------------------------------------------------------------------------
    -- 5. GET_LOW_STOCK
    -- ------------------------------------------------------------------------
    PROCEDURE GET_LOW_STOCK (
        o_cursor OUT SYS_REFCURSOR
    ) IS
    BEGIN
        OPEN o_cursor FOR
            SELECT
                p.PRODUCT_ID,
                p.SKU,
                p.PRODUCT_NAME,
                c.CATEGORY_NAME,
                NVL(s.SUPPLIER_NAME, 'Unassigned') AS SUPPLIER_NAME,
                p.PURCHASE_PRICE,
                p.SELLING_PRICE,
                p.QUANTITY,
                p.MIN_STOCK_LEVEL,
                p.STATUS,
                CASE
                    WHEN p.QUANTITY = 0 THEN 'OUT_OF_STOCK'
                    WHEN p.QUANTITY <= p.MIN_STOCK_LEVEL THEN 'LOW_STOCK'
                    ELSE 'NORMAL'
                END AS STOCK_STATUS
            FROM PRODUCTS p
            JOIN CATEGORIES c ON p.CATEGORY_ID = c.CATEGORY_ID
            LEFT JOIN SUPPLIERS s ON p.SUPPLIER_ID = s.SUPPLIER_ID
            WHERE p.QUANTITY <= p.MIN_STOCK_LEVEL
              AND p.STATUS = 'ACTIVE'
            ORDER BY p.QUANTITY ASC, p.PRODUCT_NAME ASC;
    END GET_LOW_STOCK;


    -- ------------------------------------------------------------------------
    -- 6. GET_STOCK_QUANTITY
    -- ------------------------------------------------------------------------
    FUNCTION GET_STOCK_QUANTITY (
        p_product_id IN NUMBER
    ) RETURN NUMBER IS
        v_qty NUMBER := 0;
    BEGIN
        SELECT QUANTITY
        INTO v_qty
        FROM PRODUCTS
        WHERE PRODUCT_ID = p_product_id;

        RETURN v_qty;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN -1;
    END GET_STOCK_QUANTITY;


    -- ------------------------------------------------------------------------
    -- 7. GET_TOTAL_PRODUCTS
    -- ------------------------------------------------------------------------
    FUNCTION GET_TOTAL_PRODUCTS RETURN NUMBER IS
        v_total NUMBER := 0;
    BEGIN
        SELECT COUNT(*)
        INTO v_total
        FROM PRODUCTS
        WHERE STATUS = 'ACTIVE';

        RETURN v_total;
    END GET_TOTAL_PRODUCTS;

END INVENTORY_PKG;
/
