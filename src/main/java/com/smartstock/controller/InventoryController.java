package com.smartstock.controller;

import com.smartstock.dto.ApiResponse;
import com.smartstock.dto.StockInRequest;
import com.smartstock.dto.StockOutRequest;
import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import com.smartstock.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stock-in")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stockIn(@Valid @RequestBody StockInRequest request) {
        Integer newQuantity = inventoryService.stockIn(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Stock received successfully into inventory.",
                Map.of("newQuantity", newQuantity, "productId", request.getProductId())
        ));
    }

    @PostMapping("/stock-out")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stockOut(@Valid @RequestBody StockOutRequest request) {
        Integer newQuantity = inventoryService.stockOut(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Stock issued successfully from inventory.",
                Map.of("newQuantity", newQuantity, "productId", request.getProductId())
        ));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        List<StockMovement> movements = inventoryService.getMovements(productId, movementType, search, limit);
        return ResponseEntity.ok(ApiResponse.ok(movements));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStock() {
        List<Product> lowStock = inventoryService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.ok(lowStock));
    }
}
