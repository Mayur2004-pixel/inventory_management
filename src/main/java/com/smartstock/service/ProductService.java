package com.smartstock.service;

import com.smartstock.dto.ProductRequest;
import com.smartstock.exception.BusinessRuleException;
import com.smartstock.exception.ResourceNotFoundException;
import com.smartstock.model.Product;
import com.smartstock.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts(String search, Long categoryId, String status) {
        return productRepository.findAll(search, categoryId, status);
    }

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found."));
    }

    @Transactional
    public Long createProduct(ProductRequest request) {
        // Business Rule 1 & 2: Validations
        if (request.getSku() == null || request.getSku().trim().isEmpty()) {
            throw new BusinessRuleException("Product SKU cannot be blank.");
        }
        if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
            throw new BusinessRuleException("Product name cannot be blank.");
        }
        productRepository.findBySku(request.getSku().trim()).ifPresent(p -> {
            throw new BusinessRuleException("Product SKU '" + request.getSku().trim() + "' is already in use.");
        });

        // Calls Oracle PL/SQL INVENTORY_PKG.ADD_PRODUCT
        return productRepository.addProductViaProcedure(request);
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest request) {
        getProductById(productId);
        productRepository.findBySku(request.getSku().trim()).ifPresent(p -> {
            if (!p.getProductId().equals(productId)) {
                throw new BusinessRuleException("Product SKU '" + request.getSku().trim() + "' is already in use by another product.");
            }
        });

        // Calls Oracle PL/SQL INVENTORY_PKG.UPDATE_PRODUCT
        productRepository.updateProductViaProcedure(productId, request);
    }

    @Transactional
    public void deactivateProduct(Long productId) {
        getProductById(productId);
        productRepository.updateStatus(productId, "INACTIVE");
    }

    @Transactional
    public void activateProduct(Long productId) {
        getProductById(productId);
        productRepository.updateStatus(productId, "ACTIVE");
    }

    @Transactional
    public void deleteOrDeactivate(Long productId) {
        Product p = getProductById(productId);
        try {
            // Attempt delete: If historical movements exist, TRG_PREVENT_PRODUCT_DELETE trigger will fire
            productRepository.deleteProduct(productId);
        } catch (Exception ex) {
            // If historical movements exist, auto fallback to soft-delete deactivation
            productRepository.updateStatus(productId, "INACTIVE");
        }
    }

    public Integer getStockQuantity(Long productId) {
        return productRepository.getStockQuantityViaFunction(productId);
    }

    public List<Product> getLowStockAlerts() {
        return productRepository.getLowStockViaProcedure();
    }
}
