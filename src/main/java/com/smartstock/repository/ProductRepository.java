package com.smartstock.repository;

import com.smartstock.dto.ProductRequest;
import com.smartstock.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;

@Repository
public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product p = new Product();
        p.setProductId(rs.getLong("PRODUCT_ID"));
        p.setSku(rs.getString("SKU"));
        p.setProductName(rs.getString("PRODUCT_NAME"));
        p.setCategoryId(rs.getLong("CATEGORY_ID"));
        p.setCategoryName(rs.getString("CATEGORY_NAME"));
        long supId = rs.getLong("SUPPLIER_ID");
        if (!rs.wasNull()) {
            p.setSupplierId(supId);
        }
        p.setSupplierName(rs.getString("SUPPLIER_NAME"));
        p.setPurchasePrice(rs.getBigDecimal("PURCHASE_PRICE"));
        p.setSellingPrice(rs.getBigDecimal("SELLING_PRICE"));
        p.setQuantity(rs.getInt("QUANTITY"));
        p.setMinStockLevel(rs.getInt("MIN_STOCK_LEVEL"));
        p.setStatus(rs.getString("STATUS"));
        if (rs.getTimestamp("CREATED_AT") != null) {
            p.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
        }
        if (rs.getTimestamp("UPDATED_AT") != null) {
            p.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        }
        try {
            p.setStockStatus(rs.getString("STOCK_STATUS"));
        } catch (Exception ignored) {
            // Computed dynamically if column not in query
        }
        return p;
    };

    /**
     * Executes PL/SQL Procedure: INVENTORY_PKG.ADD_PRODUCT
     */
    public Long addProductViaProcedure(ProductRequest req) {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{call INVENTORY_PKG.ADD_PRODUCT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setString(1, req.getSku());
                cs.setString(2, req.getProductName());
                cs.setLong(3, req.getCategoryId());
                if (req.getSupplierId() != null) {
                    cs.setLong(4, req.getSupplierId());
                } else {
                    cs.setNull(4, Types.NUMERIC);
                }
                cs.setBigDecimal(5, req.getPurchasePrice());
                cs.setBigDecimal(6, req.getSellingPrice());
                cs.setInt(7, req.getInitialQuantity() != null ? req.getInitialQuantity() : 0);
                cs.setInt(8, req.getMinStockLevel() != null ? req.getMinStockLevel() : 5);
                cs.setString(9, req.getStatus() != null ? req.getStatus() : "ACTIVE");
                cs.registerOutParameter(10, Types.NUMERIC);

                cs.execute();
                return cs.getLong(10);
            }
        });
    }

    /**
     * Executes PL/SQL Procedure: INVENTORY_PKG.UPDATE_PRODUCT
     */
    public void updateProductViaProcedure(Long productId, ProductRequest req) {
        jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{call INVENTORY_PKG.UPDATE_PRODUCT(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setLong(1, productId);
                cs.setString(2, req.getSku());
                cs.setString(3, req.getProductName());
                cs.setLong(4, req.getCategoryId());
                if (req.getSupplierId() != null) {
                    cs.setLong(5, req.getSupplierId());
                } else {
                    cs.setNull(5, Types.NUMERIC);
                }
                cs.setBigDecimal(6, req.getPurchasePrice());
                cs.setBigDecimal(7, req.getSellingPrice());
                cs.setInt(8, req.getMinStockLevel() != null ? req.getMinStockLevel() : 5);
                cs.setString(9, req.getStatus() != null ? req.getStatus() : "ACTIVE");

                cs.execute();
                return null;
            }
        });
    }

    /**
     * Executes PL/SQL Function: INVENTORY_PKG.GET_STOCK_QUANTITY
     */
    public Integer getStockQuantityViaFunction(Long productId) {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{? = call INVENTORY_PKG.GET_STOCK_QUANTITY(?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setLong(2, productId);
                cs.execute();
                return cs.getInt(1);
            }
        });
    }

    /**
     * Executes PL/SQL Function: INVENTORY_PKG.GET_TOTAL_PRODUCTS
     */
    public Long getTotalProductsViaFunction() {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{? = call INVENTORY_PKG.GET_TOTAL_PRODUCTS()}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.execute();
                return cs.getLong(1);
            }
        });
    }

    /**
     * Executes PL/SQL Procedure with REF CURSOR: INVENTORY_PKG.GET_LOW_STOCK
     */
    public List<Product> getLowStockViaProcedure() {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{call INVENTORY_PKG.GET_LOW_STOCK(?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.registerOutParameter(1, oracle.jdbc.OracleTypes.CURSOR);
                cs.execute();
                List<Product> list = new ArrayList<>();
                try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                    while (rs.next()) {
                        Product p = new Product();
                        p.setProductId(rs.getLong("PRODUCT_ID"));
                        p.setSku(rs.getString("SKU"));
                        p.setProductName(rs.getString("PRODUCT_NAME"));
                        p.setCategoryName(rs.getString("CATEGORY_NAME"));
                        p.setSupplierName(rs.getString("SUPPLIER_NAME"));
                        p.setPurchasePrice(rs.getBigDecimal("PURCHASE_PRICE"));
                        p.setSellingPrice(rs.getBigDecimal("SELLING_PRICE"));
                        p.setQuantity(rs.getInt("QUANTITY"));
                        p.setMinStockLevel(rs.getInt("MIN_STOCK_LEVEL"));
                        p.setStatus(rs.getString("STATUS"));
                        p.setStockStatus(rs.getString("STOCK_STATUS"));
                        list.add(p);
                    }
                }
                return list;
            }
        });
    }

    public List<Product> findAll(String search, Long categoryId, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.PRODUCT_ID, p.SKU, p.PRODUCT_NAME, p.CATEGORY_ID, c.CATEGORY_NAME,
                   p.SUPPLIER_ID, NVL(s.SUPPLIER_NAME, 'Unassigned') AS SUPPLIER_NAME,
                   p.PURCHASE_PRICE, p.SELLING_PRICE, p.QUANTITY, p.MIN_STOCK_LEVEL,
                   p.STATUS, p.CREATED_AT, p.UPDATED_AT,
                   CASE
                       WHEN p.QUANTITY = 0 THEN 'OUT_OF_STOCK'
                       WHEN p.QUANTITY <= p.MIN_STOCK_LEVEL THEN 'LOW_STOCK'
                       ELSE 'NORMAL'
                   END AS STOCK_STATUS
            FROM PRODUCTS p
            JOIN CATEGORIES c ON p.CATEGORY_ID = c.CATEGORY_ID
            LEFT JOIN SUPPLIERS s ON p.SUPPLIER_ID = s.SUPPLIER_ID
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (UPPER(p.PRODUCT_NAME) LIKE UPPER(?) OR UPPER(p.SKU) LIKE UPPER(?))");
            String likeParam = "%" + search.trim() + "%";
            params.add(likeParam);
            params.add(likeParam);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.CATEGORY_ID = ?");
            params.add(categoryId);
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append(" AND p.STATUS = ?");
            params.add(status.toUpperCase().trim());
        }

        sql.append(" ORDER BY p.PRODUCT_ID DESC");

        return jdbcTemplate.query(sql.toString(), productRowMapper, params.toArray());
    }

    public Optional<Product> findById(Long productId) {
        String sql = """
            SELECT p.PRODUCT_ID, p.SKU, p.PRODUCT_NAME, p.CATEGORY_ID, c.CATEGORY_NAME,
                   p.SUPPLIER_ID, NVL(s.SUPPLIER_NAME, 'Unassigned') AS SUPPLIER_NAME,
                   p.PURCHASE_PRICE, p.SELLING_PRICE, p.QUANTITY, p.MIN_STOCK_LEVEL,
                   p.STATUS, p.CREATED_AT, p.UPDATED_AT,
                   CASE
                       WHEN p.QUANTITY = 0 THEN 'OUT_OF_STOCK'
                       WHEN p.QUANTITY <= p.MIN_STOCK_LEVEL THEN 'LOW_STOCK'
                       ELSE 'NORMAL'
                   END AS STOCK_STATUS
            FROM PRODUCTS p
            JOIN CATEGORIES c ON p.CATEGORY_ID = c.CATEGORY_ID
            LEFT JOIN SUPPLIERS s ON p.SUPPLIER_ID = s.SUPPLIER_ID
            WHERE p.PRODUCT_ID = ?
            """;
        List<Product> list = jdbcTemplate.query(sql, productRowMapper, productId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Product> findBySku(String sku) {
        String sql = """
            SELECT p.PRODUCT_ID, p.SKU, p.PRODUCT_NAME, p.CATEGORY_ID, c.CATEGORY_NAME,
                   p.SUPPLIER_ID, NVL(s.SUPPLIER_NAME, 'Unassigned') AS SUPPLIER_NAME,
                   p.PURCHASE_PRICE, p.SELLING_PRICE, p.QUANTITY, p.MIN_STOCK_LEVEL,
                   p.STATUS, p.CREATED_AT, p.UPDATED_AT,
                   CASE
                       WHEN p.QUANTITY = 0 THEN 'OUT_OF_STOCK'
                       WHEN p.QUANTITY <= p.MIN_STOCK_LEVEL THEN 'LOW_STOCK'
                       ELSE 'NORMAL'
                   END AS STOCK_STATUS
            FROM PRODUCTS p
            JOIN CATEGORIES c ON p.CATEGORY_ID = c.CATEGORY_ID
            LEFT JOIN SUPPLIERS s ON p.SUPPLIER_ID = s.SUPPLIER_ID
            WHERE UPPER(p.SKU) = UPPER(?)
            """;
        List<Product> list = jdbcTemplate.query(sql, productRowMapper, sku.trim());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void updateStatus(Long productId, String status) {
        String sql = "UPDATE PRODUCTS SET STATUS = ? WHERE PRODUCT_ID = ?";
        jdbcTemplate.update(sql, status.toUpperCase().trim(), productId);
    }

    public void deleteProduct(Long productId) {
        // May trigger TRG_PREVENT_PRODUCT_DELETE if movements exist
        String sql = "DELETE FROM PRODUCTS WHERE PRODUCT_ID = ?";
        jdbcTemplate.update(sql, productId);
    }

    public long getTotalStockUnits() {
        Long sum = jdbcTemplate.queryForObject("SELECT NVL(SUM(QUANTITY), 0) FROM PRODUCTS WHERE STATUS = 'ACTIVE'", Long.class);
        return sum != null ? sum : 0L;
    }

    public long getOutOfStockCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PRODUCTS WHERE QUANTITY = 0 AND STATUS = 'ACTIVE'", Long.class);
        return count != null ? count : 0L;
    }

    public BigDecimal getTotalInventoryValue() {
        BigDecimal val = jdbcTemplate.queryForObject(
                "SELECT NVL(SUM(QUANTITY * PURCHASE_PRICE), 0) FROM PRODUCTS WHERE STATUS = 'ACTIVE'",
                BigDecimal.class
        );
        return val != null ? val : BigDecimal.ZERO;
    }

    public Map<String, Long> getCategoryStockDistribution() {
        String sql = """
            SELECT c.CATEGORY_NAME, NVL(SUM(p.QUANTITY), 0) AS TOTAL_QTY
            FROM CATEGORIES c
            LEFT JOIN PRODUCTS p ON c.CATEGORY_ID = p.CATEGORY_ID AND p.STATUS = 'ACTIVE'
            GROUP BY c.CATEGORY_NAME
            ORDER BY TOTAL_QTY DESC
            """;
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getString("CATEGORY_NAME"), rs.getLong("TOTAL_QTY"));
        });
        return result;
    }
}
