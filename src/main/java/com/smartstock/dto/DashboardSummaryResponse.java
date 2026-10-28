package com.smartstock.dto;

import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {
    private long totalProducts;
    private long totalStockUnits;
    private long lowStockCount;
    private long outOfStockCount;
    private long totalSuppliers;
    private BigDecimal totalInventoryValue;
    private List<StockMovement> recentMovements;
    private List<Product> lowStockProducts;
    private Map<String, Long> categoryStockDistribution;
    private Map<String, Long> movementTrend; // Stock In vs Stock Out counts

    public DashboardSummaryResponse() {}

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalStockUnits() {
        return totalStockUnits;
    }

    public void setTotalStockUnits(long totalStockUnits) {
        this.totalStockUnits = totalStockUnits;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public long getTotalSuppliers() {
        return totalSuppliers;
    }

    public void setTotalSuppliers(long totalSuppliers) {
        this.totalSuppliers = totalSuppliers;
    }

    public BigDecimal getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public List<StockMovement> getRecentMovements() {
        return recentMovements;
    }

    public void setRecentMovements(List<StockMovement> recentMovements) {
        this.recentMovements = recentMovements;
    }

    public List<Product> getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(List<Product> lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public Map<String, Long> getCategoryStockDistribution() {
        return categoryStockDistribution;
    }

    public void setCategoryStockDistribution(Map<String, Long> categoryStockDistribution) {
        this.categoryStockDistribution = categoryStockDistribution;
    }

    public Map<String, Long> getMovementTrend() {
        return movementTrend;
    }

    public void setMovementTrend(Map<String, Long> movementTrend) {
        this.movementTrend = movementTrend;
    }
}
