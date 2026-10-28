# SmartStock — College Viva Questions & Answers Guide

This comprehensive guide prepares students for college viva examinations, covering core concepts across **Java, Spring Boot, JDBC, Oracle SQL, PL/SQL, Triggers, Procedures, and Transaction Management**.

---

## Section 1: Oracle SQL & Relational Database Design

### Q1: What is the purpose of Identity columns in Oracle Database, and how does SmartStock use them?
**Answer:**  
An Identity column (`GENERATED ALWAYS AS IDENTITY`) automatically generates sequential numeric primary keys for new rows using an underlying Oracle sequence.  
In SmartStock, tables like `CATEGORIES`, `SUPPLIERS`, `PRODUCTS`, and `STOCK_MOVEMENTS` use `NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) PRIMARY KEY`. This ensures unique surrogate keys without needing manual sequence queries in Java code.

### Q2: What is the difference between Primary Key, Foreign Key, and Check Constraints?
**Answer:**  
- **Primary Key (PK):** Uniquely identifies each row in a table and automatically enforces both `NOT NULL` and `UNIQUE` constraints (e.g., `PRODUCT_ID`).
- **Foreign Key (FK):** Enforces referential integrity between two tables by ensuring that a child table's value corresponds to an existing parent key (e.g., `PRODUCTS.CATEGORY_ID REFERENCES CATEGORIES(CATEGORY_ID)`).
- **Check Constraint (CHK):** Validates column values against a boolean expression at insertion/update time. For example, `CHECK (QUANTITY >= 0)` and `CHECK (PURCHASE_PRICE >= 0)` prevent corrupt or negative stock numbers directly at the database engine tier.

### Q3: Why are indexes used on foreign key columns and timestamps in SmartStock?
**Answer:**  
Oracle does not automatically create indexes on foreign key columns. By creating indexes such as `IDX_PROD_CAT` on `PRODUCTS(CATEGORY_ID)` and `IDX_MOV_PROD` on `STOCK_MOVEMENTS(PRODUCT_ID)`:
1. Join operations during product and movement queries execute much faster via index lookups rather than full table scans.
2. It prevents full-table share locks on the child table when deleting parent rows.
3. Indexing `STOCK_MOVEMENTS(CREATED_AT DESC)` makes dashboard queries for the most recent movements almost instantaneous.

---

## Section 2: Oracle PL/SQL & Packages

### Q4: What is PL/SQL, and why use PL/SQL packages instead of writing raw SQL queries inside Java?
**Answer:**  
PL/SQL (Procedural Language/SQL) is Oracle’s proprietary extension for SQL that provides procedural features such as variables, conditional branches (`IF-ELSE`), loops, cursors, and structured exception handling.  
**Advantages of using PL/SQL packages:**
1. **Network Performance:** Instead of sending multiple round-trip SQL queries from Java over the network, a single stored procedure call executes the entire business transaction inside the database engine.
2. **Encapsulation & Security:** Complex business logic and tables are protected behind controlled procedure interfaces (`INVENTORY_PKG.STOCK_IN`, `STOCK_OUT`).
3. **Pre-compilation & Memory Caching:** The package specification and body are pre-compiled and cached in the Oracle System Global Area (SGA) shared pool for high execution speed.
4. **Maintenance:** Schema or business rule updates can be deployed directly in the database without recompiling and redeploying the Java application jar.

### Q5: What is the difference between a Stored Procedure and a Function in PL/SQL?
**Answer:**  
- **Function:** Must return a single value via the `RETURN` clause. Can be called directly in SQL statements (`SELECT INVENTORY_PKG.GET_TOTAL_PRODUCTS() FROM DUAL`).
- **Procedure:** Does not return a value via `RETURN`, but can return zero, one, or multiple values through `OUT` or `IN OUT` parameters (e.g., `o_new_quantity OUT NUMBER` in `STOCK_IN`).

### Q6: What is a `SYS_REFCURSOR` in PL/SQL, and how does Java consume it?
**Answer:**  
A `SYS_REFCURSOR` is a dynamic cursor reference (pointer to a query result set) created in PL/SQL and passed back to the client application.  
In `INVENTORY_PKG.GET_LOW_STOCK(o_cursor OUT SYS_REFCURSOR)`, Oracle opens the cursor query for products whose quantity is below threshold. In Java, Spring JDBC / JDBC registers the parameter as `OracleTypes.CURSOR` and casts it to a standard `java.sql.ResultSet` for easy iteration.

---

## Section 3: Concurrency, Locking & Transactions

### Q7: What is pessimistic locking, and how does `SELECT ... FOR UPDATE` prevent race conditions in `STOCK_OUT`?
**Answer:**  
Pessimistic locking assumes conflicting concurrent transactions are likely, so it locks the target record as soon as it is read, holding the lock until the transaction commits or rolls back.  
In `INVENTORY_PKG.STOCK_OUT`:
```sql
SELECT QUANTITY INTO v_prev_qty FROM PRODUCTS WHERE PRODUCT_ID = p_product_id FOR UPDATE;
```
If two simultaneous requests try to issue stock for the same product at the exact same millisecond:
1. The first transaction locks the row.
2. The second transaction pauses and waits until the first transaction finishes.
3. Once the lock is released, the second transaction reads the *new*, updated quantity and safely evaluates whether sufficient stock remains. This eliminates lost updates and negative stock balances.

### Q8: What are the ACID properties in database transactions?
**Answer:**  
- **Atomicity:** All operations within a transaction succeed or all fail together (all-or-nothing). In `STOCK_IN`, the product quantity update and the transaction movement record either both commit or both roll back.
- **Consistency:** Transactions transition the database from one valid state to another, upholding all constraints and triggers.
- **Isolation:** Intermediate results of an uncommitted transaction are invisible to other concurrent transactions.
- **Durability:** Once committed, transaction changes survive power outages and crashes.

### Q9: Why should partial commits be avoided inside reusable PL/SQL procedures?
**Answer:**  
If a stored procedure executes an explicit `COMMIT` internally, the calling Java service loses control of transaction boundaries. If a subsequent step in the business process fails, Java cannot roll back the already committed procedure changes. Therefore, SmartStock keeps transaction management coordinated at the Java service layer with `@Transactional`.

---

## Section 4: Database Triggers

### Q10: What is a Database Trigger, and what types of triggers are implemented in SmartStock?
**Answer:**  
A database trigger is a named PL/SQL block that automatically executes (fires) in response to a specified DDL or DML event (`INSERT`, `UPDATE`, `DELETE`) on a table.  
SmartStock implements:
1. **`TRG_PRODUCT_UPDATE_TIMESTAMP`:** A `BEFORE UPDATE` row-level trigger that sets `:NEW.UPDATED_AT := CURRENT_TIMESTAMP`.
2. **`TRG_PREVENT_PRODUCT_DELETE`:** A `BEFORE DELETE` row-level trigger that inspects `STOCK_MOVEMENTS`. If historical records exist for that product, it aborts the deletion with `RAISE_APPLICATION_ERROR(-20010, ...)` to preserve audit integrity.
3. **`TRG_PREVENT_HISTORY_MUTATION`:** A `BEFORE UPDATE OR DELETE` trigger on `STOCK_MOVEMENTS` that raises an error whenever anyone attempts to modify or erase audit history.

### Q11: What is the difference between `:NEW` and `:OLD` bind variables in triggers?
**Answer:**  
- `:OLD` represents the column value *before* the modification (available in `UPDATE` and `DELETE` operations).
- `:NEW` represents the column value *after* the modification (available in `INSERT` and `UPDATE` operations).

---

## Section 5: Java, Spring Boot & JDBC

### Q12: What is Spring Boot, and why use it for this project?
**Answer:**  
Spring Boot is an opinionated, production-ready framework that simplifies Java enterprise application development through auto-configuration, starter dependencies (`spring-boot-starter-web`, `spring-boot-starter-jdbc`), an embedded web server (Tomcat), and built-in production metrics. It eliminates boilerplate XML configuration and enables rapid setup of RESTful microservices.

### Q13: How does Spring Boot connect to Oracle Database?
**Answer:**  
1. The Oracle JDBC driver (`com.oracle.database.jdbc:ojdbc11`) provides low-level network communication with Oracle Database.
2. In `application.properties`, we configure the connection URL (`jdbc:oracle:thin:@//db.freesql.com:1521/26ai_un3c1`), username, and password.
3. Spring Boot initializes **HikariCP**, a high-performance connection pool that manages open database connections, reducing latency by reusing pre-established sockets.
4. **`JdbcTemplate`** abstracts boilerplate JDBC code (opening connections, handling `SQLException`, closing resources) and handles queries and stored procedure calls via `CallableStatement`.

### Q14: How does `GlobalExceptionHandler` translate Oracle database errors to user-friendly messages?
**Answer:**  
When an Oracle stored procedure raises an application error (e.g. `RAISE_APPLICATION_ERROR(-20043, 'Insufficient stock...')`), the JDBC driver throws a `SQLException`, which Spring wraps in a `DataAccessException`.  
Our `@RestControllerAdvice` class (`GlobalExceptionHandler`) intercepts this exception, parses out the clean message following `ORA-20xxx:`, and returns a structured JSON payload with HTTP status code `400 Bad Request` or `409 Conflict`, rather than exposing ugly stack traces to the user.

---

## Section 6: Frontend & UI/UX Design

### Q15: How does the frontend avoid static mockups?
**Answer:**  
Every single component in the user interface—from dashboard metric cards, Chart.js pie and bar charts, product tables, dropdown selectors, to movement logs—is populated asynchronously via modern JavaScript `fetch()` API calls to our Spring Boot REST endpoints. If a new product is added or stock is issued in the database, the UI updates dynamically upon loading the latest live data.

### Q16: How is Chart.js integrated into the dashboard?
**Answer:**  
Chart.js is initialized on `<canvas>` elements. When `GET /api/dashboard/summary` resolves, the returned data structures (`categoryStockDistribution` and `movementTrend`) are supplied directly to Chart.js dataset properties, dynamically rendering doughnut and bar charts with smooth animations and responsive tooltips.
