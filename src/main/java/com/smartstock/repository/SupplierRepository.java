package com.smartstock.repository;

import com.smartstock.dto.SupplierRequest;
import com.smartstock.model.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class SupplierRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupplierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Supplier> rowMapper = (rs, rowNum) -> {
        Supplier s = new Supplier(
                rs.getLong("SUPPLIER_ID"),
                rs.getString("SUPPLIER_NAME"),
                rs.getString("PHONE"),
                rs.getString("EMAIL"),
                rs.getString("ADDRESS"),
                rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null
        );
        try {
            s.setAssociatedProductsCount(rs.getInt("PRODUCT_COUNT"));
        } catch (Exception ignored) {
            // PRODUCT_COUNT may not be in all queries
        }
        return s;
    };

    public List<Supplier> findAll() {
        String sql = """
            SELECT s.SUPPLIER_ID, s.SUPPLIER_NAME, s.PHONE, s.EMAIL, s.ADDRESS, s.CREATED_AT,
                   COUNT(p.PRODUCT_ID) AS PRODUCT_COUNT
            FROM SUPPLIERS s
            LEFT JOIN PRODUCTS p ON s.SUPPLIER_ID = p.SUPPLIER_ID
            GROUP BY s.SUPPLIER_ID, s.SUPPLIER_NAME, s.PHONE, s.EMAIL, s.ADDRESS, s.CREATED_AT
            ORDER BY s.SUPPLIER_NAME ASC
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Supplier> findById(Long id) {
        String sql = """
            SELECT s.SUPPLIER_ID, s.SUPPLIER_NAME, s.PHONE, s.EMAIL, s.ADDRESS, s.CREATED_AT,
                   COUNT(p.PRODUCT_ID) AS PRODUCT_COUNT
            FROM SUPPLIERS s
            LEFT JOIN PRODUCTS p ON s.SUPPLIER_ID = p.SUPPLIER_ID
            WHERE s.SUPPLIER_ID = ?
            GROUP BY s.SUPPLIER_ID, s.SUPPLIER_NAME, s.PHONE, s.EMAIL, s.ADDRESS, s.CREATED_AT
            """;
        List<Supplier> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(SupplierRequest req) {
        String sql = "INSERT INTO SUPPLIERS (SUPPLIER_NAME, PHONE, EMAIL, ADDRESS) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"SUPPLIER_ID"});
            ps.setString(1, req.getSupplierName().trim());
            ps.setString(2, req.getPhone() != null ? req.getPhone().trim() : null);
            ps.setString(3, req.getEmail() != null ? req.getEmail().trim() : null);
            ps.setString(4, req.getAddress() != null ? req.getAddress().trim() : null);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void update(Long id, SupplierRequest req) {
        String sql = "UPDATE SUPPLIERS SET SUPPLIER_NAME = ?, PHONE = ?, EMAIL = ?, ADDRESS = ? WHERE SUPPLIER_ID = ?";
        jdbcTemplate.update(sql,
                req.getSupplierName().trim(),
                req.getPhone() != null ? req.getPhone().trim() : null,
                req.getEmail() != null ? req.getEmail().trim() : null,
                req.getAddress() != null ? req.getAddress().trim() : null,
                id
        );
    }

    public void delete(Long id) {
        String sql = "DELETE FROM SUPPLIERS WHERE SUPPLIER_ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public long count() {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SUPPLIERS", Integer.class);
        return cnt != null ? cnt : 0L;
    }
}
