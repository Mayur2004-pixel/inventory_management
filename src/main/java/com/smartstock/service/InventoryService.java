package com.smartstock.service;

import com.smartstock.dto.StockInRequest;
import com.smartstock.dto.StockOutRequest;
import com.smartstock.exception.BusinessRuleException;
import com.smartstock.exception.InsufficientStockException;
import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import com.smartstock.repository.ProductRepository;
import com.smartstock.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public InventoryService(StockMovementRepository stockMovementRepository, ProductRepository productRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    /**
     * Executes Stock In receiving operation via Oracle PL/SQL INVENTORY_PKG.STOCK_IN
     */
    @Transactional
    public Integer stockIn(StockInRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessRuleException("Stock In quantity must be strictly greater than zero.");
        }
        productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessRuleException("Target product not found."));

        return stockMovementRepository.stockInViaProcedure(request);
    }

    /**
     * Executes Stock Out sales/issue operation via Oracle PL/SQL INVENTORY_PKG.STOCK_OUT
     */
    @Transactional
    public Integer stockOut(StockOutRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessRuleException("Stock Out quantity must be strictly greater than zero.");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessRuleException("Target product not found."));

        if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new BusinessRuleException("Cannot issue stock: Product is INACTIVE.");
        }

        if (product.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for " + product.getProductName() +
                    ". Available: " + product.getQuantity() +
                    ", Requested: " + request.getQuantity()
            );
        }

        return stockMovementRepository.stockOutViaProcedure(request);
    }

    public List<StockMovement> getMovements(Long productId, String movementType, String search, int limit) {
        return stockMovementRepository.findAll(productId, movementType, search, limit);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.getLowStockViaProcedure();
    }
}
