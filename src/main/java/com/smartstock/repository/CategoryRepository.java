package com.smartstock.repository;

import com.smartstock.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Category> rowMapper = (rs, rowNum) -> new Category(
            rs.getLong("CATEGORY_ID"),
            rs.getString("CATEGORY_NAME"),
            rs.getString("DESCRIPTION"),
            rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null
    );

    public List<Category> findAll() {
        String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, DESCRIPTION, CREATED_AT FROM CATEGORIES ORDER BY CATEGORY_NAME ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Category> findById(Long id) {
        String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, DESCRIPTION, CREATED_AT FROM CATEGORIES WHERE CATEGORY_ID = ?";
        List<Category> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Category> findByName(String name) {
        String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, DESCRIPTION, CREATED_AT FROM CATEGORIES WHERE UPPER(CATEGORY_NAME) = UPPER(?)";
        List<Category> list = jdbcTemplate.query(sql, rowMapper, name);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(String name, String description) {
        String sql = "INSERT INTO CATEGORIES (CATEGORY_NAME, DESCRIPTION) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, new String[]{"CATEGORY_ID"});
            ps.setString(1, name.trim());
            ps.setString(2, description != null ? description.trim() : null);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void update(Long id, String name, String description) {
        String sql = "UPDATE CATEGORIES SET CATEGORY_NAME = ?, DESCRIPTION = ? WHERE CATEGORY_ID = ?";
        jdbcTemplate.update(sql, name.trim(), description != null ? description.trim() : null, id);
    }

    public void delete(Long id) {
        String sql = "DELETE FROM CATEGORIES WHERE CATEGORY_ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
