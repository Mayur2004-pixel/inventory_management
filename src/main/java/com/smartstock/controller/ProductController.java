package com.smartstock.controller;

import com.smartstock.dto.ApiResponse;
import com.smartstock.dto.ProductRequest;
import com.smartstock.model.Product;
import com.smartstock.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        List<Product> products = productService.getAllProducts(search, categoryId, status);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProduct(@Valid @RequestBody ProductRequest request) {
        Long productId = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created successfully", Map.of("productId", productId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteOrDeactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted or deactivated successfully", null));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deactivated successfully", null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product activated successfully", null));
    }

    @GetMapping("/{id}/quantity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductQuantity(@PathVariable Long id) {
        Integer qty = productService.getStockQuantity(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("productId", id, "quantity", qty)));
    }
}
