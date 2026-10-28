# SmartStock — REST API Documentation

The SmartStock backend exposes a set of RESTful endpoints built with Spring Boot 3.3. All transactional endpoints directly execute compiled **Oracle PL/SQL Procedures & Functions** inside `INVENTORY_PKG`.

Base URL: `http://localhost:8080`

All responses follow the standard `ApiResponse<T>` envelope:
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": "2026-10-27T21:14:07"
}
```

---

## 1. Dashboard Endpoints

### `GET /api/dashboard/summary`
- **Description**: Returns real-time executive inventory metrics, stock distributions, movement trends, and low-stock alerts.
- **Underlying PL/SQL**: Calls `INVENTORY_PKG.GET_TOTAL_PRODUCTS()`, `INVENTORY_PKG.GET_LOW_STOCK()`, and aggregate queries.
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "totalProducts": 11,
    "totalStockUnits": 324,
    "lowStockCount": 2,
    "outOfStockCount": 0,
    "totalSuppliers": 4,
    "totalInventoryValue": 8149.90,
    "recentMovements": [ ... ],
    "lowStockProducts": [ ... ],
    "categoryStockDistribution": {
      "Stationery": 198,
      "Computer Accessories": 64,
      "Electronics": 43,
      "Furniture": 16,
      "Networking": 3
    },
    "movementTrend": {
      "STOCK_IN": 9,
      "STOCK_OUT": 8
    }
  }
}
```

---

## 2. Product Management Endpoints

### `GET /api/products`
- **Description**: Retrieve paginated/filtered list of catalog items.
- **Query Parameters**:
  - `search` (optional): Filter by name or SKU
  - `categoryId` (optional): Filter by numeric category ID
  - `status` (optional): Filter by `ACTIVE`, `INACTIVE`, or `ALL` (default)
- **Response `200 OK`**: Array of `Product` objects.

### `GET /api/products/{id}`
- **Description**: Fetch full details of an individual product.
- **Response `200 OK`**: Single `Product` object.
- **Error `404 Not Found`**: When ID does not exist.

### `POST /api/products`
- **Description**: Registers a new product into Oracle DB.
- **Underlying PL/SQL**: Executes `INVENTORY_PKG.ADD_PRODUCT(...)`.
- **Request Body**:
```json
{
  "sku": "KB-RGB-01",
  "productName": "RGB Mechanical Gaming Keyboard",
  "categoryId": 4,
  "supplierId": 4,
  "purchasePrice": 45.00,
  "sellingPrice": 89.99,
  "initialQuantity": 20,
  "minStockLevel": 8,
  "status": "ACTIVE"
}
```
- **Response `201 Created`**:
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { "productId": 12 }
}
```
- **Error `409 Conflict`**: If SKU is already in use (`ORA-20003` / `ORA-00001`).

### `PUT /api/products/{id}`
- **Description**: Updates product attributes, pricing, and thresholds.
- **Underlying PL/SQL**: Executes `INVENTORY_PKG.UPDATE_PRODUCT(...)`.
- **Response `200 OK`**: Success message.

### `DELETE /api/products/{id}`
- **Description**: Deletes or safely deactivates a product.
- **Logic**: If the product has historical movement logs, database trigger `TRG_PREVENT_PRODUCT_DELETE` intercepts physical deletion, and the backend safely updates status to `INACTIVE`.

### `PATCH /api/products/{id}/activate`
- **Description**: Reactivates an inactive product (`STATUS = 'ACTIVE'`).

---

## 3. Inventory Operations Endpoints

### `POST /api/inventory/stock-in`
- **Description**: Records incoming procurement goods into warehouse inventory.
- **Underlying PL/SQL**: Executes `INVENTORY_PKG.STOCK_IN(...)` with row-level locking.
- **Request Body**:
```json
{
  "productId": 4,
  "supplierId": 3,
  "quantity": 10,
  "unitPrice": 110.00,
  "referenceNumber": "PO-2026-9001",
  "remarks": "Restocking mesh chairs consignment"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Stock received successfully into inventory.",
  "data": {
    "productId": 4,
    "newQuantity": 10
  }
}
```

### `POST /api/inventory/stock-out`
- **Description**: Issues or sells stock from inventory.
- **Underlying PL/SQL**: Executes `INVENTORY_PKG.STOCK_OUT(...)`.
- **Concurrency Safety**: Acquires row lock via `SELECT ... FOR UPDATE` and validates `current_stock >= requested_qty`.
- **Request Body**:
```json
{
  "productId": 4,
  "quantity": 2,
  "referenceNumber": "SO-2026-4001",
  "remarks": "Dispatched to Corporate Department"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Stock issued successfully from inventory.",
  "data": {
    "productId": 4,
    "newQuantity": 8
  }
}
```
- **Error `400 Bad Request`**: When requested quantity exceeds available balance (`ORA-20043: Insufficient stock available`).

### `GET /api/inventory/movements`
- **Description**: Retrieves audit trail of stock movements.
- **Query Parameters**:
  - `productId` (optional): Filter by product
  - `movementType` (optional): `STOCK_IN` or `STOCK_OUT`
  - `search` (optional): Search by product name, SKU, or reference number
  - `limit` (optional): Max records returned (default: 100)
- **Response `200 OK`**: Array of `StockMovement` records.

### `GET /api/inventory/low-stock`
- **Description**: Retrieves all products whose current quantity is at or below minimum reorder threshold.
- **Underlying PL/SQL**: Executes `INVENTORY_PKG.GET_LOW_STOCK(o_cursor OUT SYS_REFCURSOR)`.
- **Response `200 OK`**: List of low-stock and out-of-stock products.

---

## 4. Supplier Management Endpoints

### `GET /api/suppliers`
- **Description**: Returns all registered suppliers alongside their associated product counts.
- **Response `200 OK`**: Array of `Supplier` objects.

### `POST /api/suppliers`
- **Description**: Registers a new vendor entity.
- **Request Body**:
```json
{
  "supplierName": "OmniCore Hardware",
  "phone": "+1-555-0199",
  "email": "orders@omnicore.com",
  "address": "500 Tech Parkway, Denver, CO"
}
```
- **Response `201 Created`**: Returns generated `supplierId`.

### `PUT /api/suppliers/{id}`
- **Description**: Updates vendor contact information.

### `DELETE /api/suppliers/{id}`
- **Description**: Removes supplier if no catalog items reference it.
