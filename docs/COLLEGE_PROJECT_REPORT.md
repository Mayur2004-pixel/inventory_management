# SmartStock — Inventory Management System
## College Minor Project Report & Technical Thesis

**Department of Computer Science & Engineering / Information Technology**  
**Academic Year:** 2026–2027  
**Project Title:** SmartStock — Database-Driven Inventory Management System  
**Technologies:** Java 21, Spring Boot 3.3, Oracle Database 26ai Cloud, Oracle PL/SQL, Bootstrap 5, Chart.js  

---

## Executive Summary & Abstract

Traditional inventory management systems frequently suffer from data inconsistencies, race conditions during simultaneous stock issuances, lack of permanent audit records, and reliance on client-side validation alone. **SmartStock** is an enterprise-grade, full-stack inventory management solution developed to address these fundamental deficiencies by enforcing core business rules and transactional integrity directly at the database engine tier using **Oracle PL/SQL packages, procedures, functions, and row-level locks**.

Built on a modular multi-tier architecture, the backend leverages **Spring Boot 3.3** communicating over **Oracle JDBC (`JdbcTemplate` / `CallableStatement`)** with a remote **Oracle Database 26ai Cloud instance**. The application manages product catalogs, stock levels, suppliers, stock-in receiving, stock-out sales dispatching, and auditable transaction movements. Critical concurrent transactions—such as avoiding negative inventory balances—are guaranteed using Oracle's pessimistic locking (`SELECT ... FOR UPDATE`) inside PL/SQL stored procedures.

The responsive frontend incorporates modern enterprise UI/UX standards with white/light-gray backgrounds, navy-blue accents, real-time Chart.js telemetry dashboards, low-stock visual warning triggers, and interactive CRUD interfaces.

---

## 1. Introduction & Objectives

### 1.1 Problem Statement
Inventory management is the backbone of retail, manufacturing, and supply-chain enterprises. Common systemic issues in naive systems include:
- **Phantom Reads & Race Conditions:** Concurrent users issuing stock can reduce balances below zero when validations happen outside database lock contexts.
- **Audit Tampering:** Accidental deletion or mutation of historical transaction records causes unrecoverable discrepancies in ledger balancing.
- **Orphaned References:** Hard deletion of products breaks historical procurement and dispatch analysis.
- **Understocked Bottlenecks:** Lack of automated replenishment triggers leads to stock-outs of critical items.

### 1.2 Core Project Objectives
1. **Database-Tier Business Integrity:** Delegate critical inventory checks and stock modifications to a dedicated Oracle PL/SQL package (`INVENTORY_PKG`).
2. **Concurrency Safety:** Guarantee atomic, thread-safe stock-out operations via pessimistic row-level locking (`SELECT ... FOR UPDATE`), strictly preventing negative balances under simultaneous requests.
3. **Immutable Audit Ledger:** Establish a permanent `STOCK_MOVEMENTS` log guarded by database triggers (`TRG_PREVENT_HISTORY_MUTATION`) that disallows modifications or silent deletions.
4. **Soft-Delete Lifecycle:** Enforce automated deactivation for products referenced in historical logs (`TRG_PREVENT_PRODUCT_DELETE`).
5. **Real-Time Telemetry:** Deliver an interactive executive dashboard with live KPIs, category distribution charts, and replenishment warnings.
6. **Academic Rigor:** Produce clean, modular, and beginner-friendly code ideal for technical viva defense.

---

## 2. System Architecture & Methodology

SmartStock adheres to the **3-Tier Enterprise Architectural Pattern**:

```
+-------------------------------------------------------------------------+
|                           PRESENTATION TIER                             |
|       Bootstrap 5, HTML5, CSS3 Custom Tokens, Chart.js, Vanilla JS      |
+-------------------------------------------------------------------------+
                                    |
                            HTTP / JSON REST
                                    |
+-------------------------------------------------------------------------+
|                            APPLICATION TIER                             |
|   Spring Boot 3.3 (Web MVC, HikariCP, JdbcTemplate, Global Exception)   |
|   Packages: controller -> service -> repository -> dto -> model        |
+-------------------------------------------------------------------------+
                                    |
                            Oracle JDBC (TCPS)
                                    |
+-------------------------------------------------------------------------+
|                             DATABASE TIER                               |
|                     Oracle Database 26ai Cloud                          |
|   Package: INVENTORY_PKG (Procedures, Functions, Cursors, Row-Locks)    |
|   Triggers: Immutability, Audit, Soft-Delete Enforcement                |
|   Tables: CATEGORIES, SUPPLIERS, PRODUCTS, STOCK_MOVEMENTS              |
+-------------------------------------------------------------------------+
```

### 2.1 Methodology
- **Incremental Layered Construction:** Starting with schema normalization and PL/SQL package unit testing in Oracle SQL Developer, followed by Spring Boot JDBC integration, REST endpoints, and finally the responsive UI.
- **Transaction Coordination:** Spring Boot manages transactional boundaries (`@Transactional`) while PL/SQL procedures execute atomic state changes within the connection's active transaction context.
- **Defensive Error Handling:** Custom Oracle application errors (codes `-20001` through `-20043`) are mapped to clean HTTP responses (`400 Bad Request`, `409 Conflict`, `404 Not Found`) by a centralized `@RestControllerAdvice`.

---

## 3. Database Design & Schemas

### 3.1 Relational Schema Definitions
The database schema consists of four normalized tables conforming to **Third Normal Form (3NF)**:

1. **`CATEGORIES`**:
   - `CATEGORY_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `CATEGORY_NAME VARCHAR2(100) NOT NULL UNIQUE`
   - `DESCRIPTION VARCHAR2(255)`
   - `CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

2. **`SUPPLIERS`**:
   - `SUPPLIER_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `SUPPLIER_NAME VARCHAR2(150) NOT NULL`
   - `PHONE VARCHAR2(30)`
   - `EMAIL VARCHAR2(100) CHECK (EMAIL IS NULL OR EMAIL LIKE '%_@__%.__%')`
   - `ADDRESS VARCHAR2(300)`
   - `CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

3. **`PRODUCTS`**:
   - `PRODUCT_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `SKU VARCHAR2(50) NOT NULL UNIQUE`
   - `PRODUCT_NAME VARCHAR2(150) NOT NULL`
   - `CATEGORY_ID NUMBER NOT NULL REFERENCES CATEGORIES(CATEGORY_ID)`
   - `SUPPLIER_ID NUMBER REFERENCES SUPPLIERS(SUPPLIER_ID)`
   - `PURCHASE_PRICE NUMBER(12,2) CHECK (PURCHASE_PRICE >= 0)`
   - `SELLING_PRICE NUMBER(12,2) CHECK (SELLING_PRICE >= 0)`
   - `QUANTITY NUMBER(10) DEFAULT 0 CHECK (QUANTITY >= 0)`
   - `MIN_STOCK_LEVEL NUMBER(10) DEFAULT 5 CHECK (MIN_STOCK_LEVEL >= 0)`
   - `STATUS VARCHAR2(20) DEFAULT 'ACTIVE' CHECK (STATUS IN ('ACTIVE', 'INACTIVE'))`
   - `CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
   - `UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

4. **`STOCK_MOVEMENTS`**:
   - `MOVEMENT_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `PRODUCT_ID NUMBER NOT NULL REFERENCES PRODUCTS(PRODUCT_ID)`
   - `MOVEMENT_TYPE VARCHAR2(20) CHECK (MOVEMENT_TYPE IN ('STOCK_IN', 'STOCK_OUT'))`
   - `QUANTITY NUMBER(10) CHECK (QUANTITY > 0)`
   - `PREVIOUS_QUANTITY NUMBER(10) CHECK (PREVIOUS_QUANTITY >= 0)`
   - `NEW_QUANTITY NUMBER(10) CHECK (NEW_QUANTITY >= 0)`
   - `REFERENCE_NUMBER VARCHAR2(100)`
   - `REMARKS VARCHAR2(400)`
   - `CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

---

## 4. Oracle PL/SQL Component Implementation

### 4.1 Package Specification: `INVENTORY_PKG`
The package encapsulates all mutating stock operations and executive lookups:

```sql
CREATE OR REPLACE PACKAGE INVENTORY_PKG AS
    PROCEDURE ADD_PRODUCT (...);
    PROCEDURE UPDATE_PRODUCT (...);
    PROCEDURE STOCK_IN (p_product_id, p_supplier_id, p_quantity, p_unit_price, p_reference_no, p_remarks, o_new_quantity OUT NUMBER);
    PROCEDURE STOCK_OUT (p_product_id, p_quantity, p_reference_no, p_remarks, o_new_quantity OUT NUMBER);
    PROCEDURE GET_LOW_STOCK (o_cursor OUT SYS_REFCURSOR);
    FUNCTION GET_STOCK_QUANTITY (p_product_id NUMBER) RETURN NUMBER;
    FUNCTION GET_TOTAL_PRODUCTS RETURN NUMBER;
END INVENTORY_PKG;
```

### 4.2 Key Concurrency Logic in `STOCK_OUT`
To guarantee thread-safety and avoid race conditions, row locking is acquired prior to any evaluation:

```sql
PROCEDURE STOCK_OUT (...) IS
    v_prev_qty NUMBER;
    v_new_qty  NUMBER;
    v_status   VARCHAR2(20);
BEGIN
    IF p_quantity <= 0 THEN
        RAISE_APPLICATION_ERROR(-20040, 'Stock-out quantity must be strictly greater than zero.');
    END IF;

    -- Pessimistic row locking
    SELECT QUANTITY, STATUS INTO v_prev_qty, v_status
    FROM PRODUCTS
    WHERE PRODUCT_ID = p_product_id
    FOR UPDATE;

    IF UPPER(v_status) = 'INACTIVE' THEN
        RAISE_APPLICATION_ERROR(-20042, 'Cannot issue stock: Product is INACTIVE.');
    END IF;

    IF v_prev_qty < p_quantity THEN
        RAISE_APPLICATION_ERROR(-20043, 'Insufficient stock available. Current: ' || v_prev_qty || ', Requested: ' || p_quantity);
    END IF;

    v_new_qty := v_prev_qty - p_quantity;

    UPDATE PRODUCTS SET QUANTITY = v_new_qty WHERE PRODUCT_ID = p_product_id;

    INSERT INTO STOCK_MOVEMENTS (PRODUCT_ID, MOVEMENT_TYPE, QUANTITY, PREVIOUS_QUANTITY, NEW_QUANTITY, REFERENCE_NUMBER, REMARKS)
    VALUES (p_product_id, 'STOCK_OUT', p_quantity, v_prev_qty, v_new_qty, p_reference_no, p_remarks);

    o_new_quantity := v_new_qty;
END STOCK_OUT;
```

---

## 5. Testing & Verification Results

A rigorous automated and manual test matrix was executed against the live Oracle 26ai Cloud Database.

| Test ID | Test Description | Input Data | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|
| **TC-01** | Add new product | Valid SKU, Name, Prices, Initial Qty=10 | Record saved, initial STOCK_IN movement inserted | Created Product ID, Movement inserted | **PASS** |
| **TC-02** | Duplicate SKU Rejection | Existing SKU: `KB-MECH-01` | Operation rejected with duplicate error | `ORA-20003` captured & HTTP 409 returned | **PASS** |
| **TC-03** | Valid Stock-In | Product 1 (+5 units, PO ref) | Qty increases from 33 to 38, movement recorded | Balance updated to 38 | **PASS** |
| **TC-04** | Valid Stock-Out | Product 1 (-2 units, SO ref) | Qty decreases from 38 to 36 | Balance updated to 36 | **PASS** |
| **TC-05** | Insufficient Stock Out | Product 4 (8 units in stock, request 50) | Rejection with error message, balance unchanged | `ORA-20043` thrown, balance stayed 8 | **PASS** |
| **TC-06** | Zero/Negative Qty | Stock-In qty = 0; Stock-Out qty = -5 | Validation error thrown | Rejected with 400 Bad Request | **PASS** |
| **TC-07** | Low-Stock Alerts Cursor | Query `GET_LOW_STOCK` | Returns items with `QTY <= MIN_STOCK` | Correctly identified 3 alert products | **PASS** |
| **TC-08** | Physical Deletion Prevention | Attempt `DELETE FROM PRODUCTS` with movements | Trigger `TRG_PREVENT_PRODUCT_DELETE` fires | `ORA-20010` raised, safe soft-deactivate | **PASS** |
| **TC-09** | Movement Log Immutability | Attempt `UPDATE STOCK_MOVEMENTS` | Trigger `TRG_PREVENT_HISTORY_MUTATION` fires | `ORA-20012` raised, log unmodified | **PASS** |

---

## 6. Conclusion & Future Enhancements

### 6.1 Conclusion
The **SmartStock Inventory Management System** achieves its core goal of demonstrating a robust, database-driven business application combining modern Java Spring Boot with advanced Oracle PL/SQL programming. By delegating data validation, concurrency protection, and ledger immutability to Oracle PL/SQL stored procedures and triggers, the application ensures absolute reliability regardless of concurrent client traffic.

### 6.2 Future Enhancements
- Integration with barcode and RFID scanning hardware.
- Automated email alerts to suppliers when items reach reorder threshold.
- Multi-warehouse inventory location mapping.
- Machine Learning forecasting for seasonal demand prediction.
