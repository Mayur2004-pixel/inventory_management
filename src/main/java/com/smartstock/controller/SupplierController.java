package com.smartstock.controller;

import com.smartstock.dto.ApiResponse;
import com.smartstock.dto.SupplierRequest;
import com.smartstock.model.Supplier;
import com.smartstock.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Supplier>>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(ApiResponse.ok(suppliers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.ok(supplier));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSupplier(@Valid @RequestBody SupplierRequest request) {
        Long id = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Supplier created successfully", Map.of("supplierId", id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deleted successfully", null));
    }
}
