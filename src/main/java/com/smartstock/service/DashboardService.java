package com.smartstock.service;

import com.smartstock.dto.DashboardSummaryResponse;
import com.smartstock.model.Product;
import com.smartstock.repository.ProductRepository;
import com.smartstock.repository.StockMovementRepository;
import com.smartstock.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;

    public DashboardService(ProductRepository productRepository,
                            SupplierRepository supplierRepository,
                            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public DashboardSummaryResponse getDashboardSummary() {
        DashboardSummaryResponse res = new DashboardSummaryResponse();

        res.setTotalProducts(productRepository.getTotalProductsViaFunction());
        res.setTotalStockUnits(productRepository.getTotalStockUnits());
        res.setOutOfStockCount(productRepository.getOutOfStockCount());
        res.setTotalSuppliers(supplierRepository.count());
        res.setTotalInventoryValue(productRepository.getTotalInventoryValue());

        List<Product> lowStockList = productRepository.getLowStockViaProcedure();
        res.setLowStockProducts(lowStockList);
        res.setLowStockCount(lowStockList.size());

        res.setRecentMovements(stockMovementRepository.findRecent(8));
        res.setCategoryStockDistribution(productRepository.getCategoryStockDistribution());
        res.setMovementTrend(stockMovementRepository.getMovementTypeCounts());

        return res;
    }
}
