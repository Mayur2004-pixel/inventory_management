package com.smartstock.controller;

import com.smartstock.dto.ApiResponse;
import com.smartstock.model.Category;
import com.smartstock.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getCategoryById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCategory(@RequestBody Map<String, String> body) {
        String name = body.get("categoryName");
        String description = body.get("description");
        Long id = categoryService.createCategory(name, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created successfully", Map.of("categoryId", id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("categoryName");
        String description = body.get("description");
        categoryService.updateCategory(id, name, description);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully", null));
    }
}
