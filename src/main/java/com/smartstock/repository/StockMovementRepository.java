package com.smartstock.repository;

import com.smartstock.dto.StockInRequest;
import com.smartstock.dto.StockOutRequest;
import com.smartstock.model.StockMovement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.*;

@Repository
public class StockMovementRepository {

    private static final Logger log = LoggerFactory.getLogger(StockMovementRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public StockMovementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StockMovement> rowMapper = (rs, rowNum) -> {
        StockMovement m = new StockMovement();
        m.setMovementId(rs.getLong("MOVEMENT_ID"));
        m.setProductId(rs.getLong("PRODUCT_ID"));
        m.setProductName(rs.getString("PRODUCT_NAME"));
        m.setSku(rs.getString("SKU"));
        m.setMovementType(rs.getString("MOVEMENT_TYPE"));
        m.setQuantity(rs.getInt("QUANTITY"));
        m.setPreviousQuantity(rs.getInt("PREVIOUS_QUANTITY"));
        m.setNewQuantity(rs.getInt("NEW_QUANTITY"));
        m.setReferenceNumber(rs.getString("REFERENCE_NUMBER"));
        m.setRemarks(rs.getString("REMARKS"));
        if (rs.getTimestamp("CREATED_AT") != null) {
            m.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
        }
        return m;
    };

    /**
     * Executes PL/SQL Procedure: INVENTORY_PKG.STOCK_IN
     */
    public Integer stockInViaProcedure(StockInRequest req) {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{call INVENTORY_PKG.STOCK_IN(?, ?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setLong(1, req.getProductId());
                if (req.getSupplierId() != null) {
                    cs.setLong(2, req.getSupplierId());
                } else {
                    cs.setNull(2, Types.NUMERIC);
                }
                cs.setInt(3, req.getQuantity());
                if (req.getUnitPrice() != null) {
                    cs.setBigDecimal(4, req.getUnitPrice());
                } else {
                    cs.setNull(4, Types.NUMERIC);
                }
                cs.setString(5, req.getReferenceNumber());
                cs.setString(6, req.getRemarks());
                cs.registerOutParameter(7, Types.NUMERIC);

                cs.execute();
                return cs.getInt(7);
            }
        });
    }

    /**
     * Executes PL/SQL Procedure: INVENTORY_PKG.STOCK_OUT
     */
    public Integer stockOutViaProcedure(StockOutRequest req) {
        return jdbcTemplate.execute((Connection conn) -> {
            String callSql = "{call INVENTORY_PKG.STOCK_OUT(?, ?, ?, ?, ?)}";
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setLong(1, req.getProductId());
                cs.setInt(2, req.getQuantity());
                cs.setString(3, req.getReferenceNumber());
                cs.setString(4, req.getRemarks());
                cs.registerOutParameter(5, Types.NUMERIC);

                cs.execute();
                return cs.getInt(5);
            }
        });
    }

    public List<StockMovement> findAll(Long productId, String movementType, String search, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT m.MOVEMENT_ID, m.PRODUCT_ID, p.PRODUCT_NAME, p.SKU,
                   m.MOVEMENT_TYPE, m.QUANTITY, m.PREVIOUS_QUANTITY, m.NEW_QUANTITY,
                   m.REFERENCE_NUMBER, m.REMARKS, m.CREATED_AT
            FROM STOCK_MOVEMENTS m
            JOIN PRODUCTS p ON m.PRODUCT_ID = p.PRODUCT_ID
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (productId != null && productId > 0) {
            sql.append(" AND m.PRODUCT_ID = ?");
            params.add(productId);
        }

        if (movementType != null && !movementType.trim().isEmpty() && !movementType.equalsIgnoreCase("ALL")) {
            sql.append(" AND m.MOVEMENT_TYPE = ?");
            params.add(movementType.toUpperCase().trim());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (UPPER(p.PRODUCT_NAME) LIKE UPPER(?) OR UPPER(p.SKU) LIKE UPPER(?) OR UPPER(m.REFERENCE_NUMBER) LIKE UPPER(?))");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY m.CREATED_AT DESC, m.MOVEMENT_ID DESC FETCH FIRST ? ROWS ONLY");
        params.add(limit > 0 ? limit : 100);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<StockMovement> findRecent(int limit) {
        return findAll(null, null, null, limit);
    }

    public Map<String, Long> getMovementTypeCounts() {
        String sql = "SELECT MOVEMENT_TYPE, COUNT(*) AS CNT FROM STOCK_MOVEMENTS GROUP BY MOVEMENT_TYPE";
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("STOCK_IN", 0L);
        map.put("STOCK_OUT", 0L);
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString("MOVEMENT_TYPE"), rs.getLong("CNT"));
        });
        return map;
    }
}
