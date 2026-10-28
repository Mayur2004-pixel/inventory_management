# SmartStock — Inventory Management System

A production-grade, database-driven **Inventory Management System** built with **Java 21**, **Spring Boot 3.3**, and **Oracle Database 26ai Cloud**, featuring business logic encapsulated in **Oracle PL/SQL packages (`INVENTORY_PKG`)**, database triggers for audit log immutability, and a modern enterprise dashboard with Chart.js analytics.

---

## 1. Technology Stack

- **Backend:** Java 21 / 25, Spring Boot 3.3.4, Spring JDBC (`JdbcTemplate`, `CallableStatement`).
- **Database:** Oracle Database 26ai Cloud (`db.freesql.com:1521/26ai_un3c1`).
- **Database Programming:** Oracle PL/SQL package `INVENTORY_PKG`, procedures, functions, REF CURSORs, row-level locking (`SELECT ... FOR UPDATE`), and triggers.
- **Frontend:** HTML5, CSS3 Custom Tokens, Vanilla JavaScript, Bootstrap 5.3, Chart.js, Bootstrap Icons.
- **Connection Pool:** HikariCP.
- **Build Tool:** Apache Maven 3.9+.

---

## 2. Live Application Views

The application runs locally at **`http://localhost:8080/`** with zero static mockups—all information queries the live Oracle Cloud Database:

1. **Enterprise Dashboard (`/`):**
   - 6 KPI metric cards: Total Products, Total Stock Units, Low Stock Alert, Out of Stock, Total Suppliers, Total Inventory Value.
   - Live Chart.js Category Distribution doughnut chart.
   - Audited Movement Operations ratio bar chart.
   - Live Recent Movements table and Replenishment Alert action list.
2. **Products Catalog:**
   - Full CRUD operations with instant search by Name or SKU.
   - Filter by Category and Status (`ACTIVE` / `INACTIVE`).
   - Add Product modal (calls `INVENTORY_PKG.ADD_PRODUCT`).
   - Edit Product modal (calls `INVENTORY_PKG.UPDATE_PRODUCT`).
   - Safe Deactivation / Soft-Delete lifecycle (enforced by `TRG_PREVENT_PRODUCT_DELETE`).
3. **Stock In — Receiving:**
   - Procurement receiving form with auto-filled current stock balances and unit purchase pricing.
   - Atomically updates product stock and logs transaction history via `INVENTORY_PKG.STOCK_IN`.
4. **Stock Out — Sales / Issue Dispatch:**
   - Issue form protected by Oracle pessimistic row-level locking (`SELECT FOR UPDATE`).
   - Prevents race conditions and rejects operations when requested quantity exceeds available stock (`ORA-20043`).
5. **Inventory Transaction History:**
   - Immutable audit trail of every stock addition and deduction with balance transitions (`previousQuantity -> newQuantity`).
   - Mutation and deletion strictly prevented by database trigger `TRG_PREVENT_HISTORY_MUTATION`.
6. **Low-Stock Alerts:**
   - Automatically queries products at or below threshold via PL/SQL REF CURSOR (`INVENTORY_PKG.GET_LOW_STOCK`).
   - One-click "Receive Stock" quick restock action.
7. **Suppliers Directory:**
   - Vendor contact directory with counts of associated catalog products.

---

## 3. Database Architecture & PL/SQL Package

### Tables
- `CATEGORIES` — Product category classifications.
- `SUPPLIERS` — External vendor contact details.
- `PRODUCTS` — Central catalog with live stock balances, purchase/selling pricing, and thresholds.
- `STOCK_MOVEMENTS` — Immutable transaction ledger tracking all stock-in and stock-out movements.

### PL/SQL Package: `INVENTORY_PKG`
- `ADD_PRODUCT`: Creates product and optional initial stock-in movement.
- `UPDATE_PRODUCT`: Validates uniqueness and updates product details.
- `STOCK_IN`: Pessimistic lock, increases available quantity, records movement history.
- `STOCK_OUT`: Pessimistic lock (`SELECT FOR UPDATE`), verifies stock availability, decreases quantity, records movement history. Rejects if balance insufficient.
- `GET_LOW_STOCK`: Returns dynamic `SYS_REFCURSOR` of products needing replenishment.
- `GET_STOCK_QUANTITY`: Scalar function returning real-time balance for a product.
- `GET_TOTAL_PRODUCTS`: Scalar function returning active products count.

### Triggers
- `TRG_PRODUCT_UPDATE_TIMESTAMP`: Auto-updates `UPDATED_AT` timestamp on row modification.
- `TRG_PREVENT_PRODUCT_DELETE`: Aborts hard deletion of products with historical movement records (`ORA-20010`).
- `TRG_PREVENT_HISTORY_MUTATION`: Aborts updates or deletions on `STOCK_MOVEMENTS` (`ORA-20011`, `ORA-20012`).

---

## 4. Project Directory Structure

```text
smartstock
├── database/
│   ├── 01_create_schema.sql         # DDL: Tables, constraints, sequences, indexes
│   ├── 02_create_triggers.sql       # Triggers: Timestamp update & immutability
│   ├── 03_inventory_pkg.sql         # PL/SQL: INVENTORY_PKG Spec & Body
│   └── 04_sample_data.sql           # Realistic seed data for immediate demonstration
├── docs/
│   ├── ER_DIAGRAM.md                # Entity Relationship diagram & schema specification
│   ├── API_DOCUMENTATION.md         # Complete REST API reference
│   ├── COLLEGE_PROJECT_REPORT.md    # Full academic project report / thesis
│   └── VIVA_QUESTIONS_ANSWERS.md    # Viva preparation guide for college evaluations
├── src/
│   ├── main/
│   │   ├── java/com/smartstock/
│   │   │   ├── config/              # Application configurations
│   │   │   ├── controller/          # REST Controllers (/api/...)
│   │   │   ├── service/             # Business & PL/SQL coordination services
│   │   │   ├── repository/          # Spring JdbcTemplate & CallableStatement DAOs
│   │   │   ├── dto/                 # Request & Response Data Transfer Objects
│   │   │   ├── model/               # Domain Entity Models
│   │   │   ├── exception/           # Centralized exception handler & Oracle error parser
│   │   │   └── SmartStockApplication.java # Spring Boot entrypoint
│   │   └── resources/
│   │       ├── application.properties # Oracle JDBC datasource configuration
│   │       └── static/
│   │           ├── css/styles.css   # Custom clean enterprise design tokens
│   │           ├── js/app.js        # Single-page application logic & Chart.js
│   │           └── index.html       # Responsive web application interface
│   └── test/java/com/smartstock/
│       └── SmartStockApplicationTests.java # Automated integration test suite
├── pom.xml                          # Maven build dependencies & plugins
└── README.md                        # Documentation & setup instructions
```

---

## 5. Execution & Setup Instructions

### Prerequisites
- Java Development Kit (JDK 21 or later)
- Apache Maven 3.8+
- Active Internet connection (for remote Oracle Cloud Database connectivity)

### Run Application
From the project root directory:

```bash
mvn spring-boot:run
```

Once started, open your web browser and navigate to:
```text
http://localhost:8080/
```

### Run Automated Integration Tests
To execute the automated test suite verifying all 11 business rules, PL/SQL procedures, and locking constraints against the live Oracle Database:

```bash
mvn test
```

---

## 6. Oracle Cloud Database Credentials

The application is pre-configured in `src/main/resources/application.properties` to connect to:
- **Hostname:** `db.freesql.com`
- **Port:** `1521` (TCP) / `2484` (TCPS)
- **Service Name:** `26ai_un3c1`
- **Schema User:** `MDRAZATECH_SCHEMA_GMAXC`
- **Driver:** `oracle.jdbc.OracleDriver` (`ojdbc11`)
