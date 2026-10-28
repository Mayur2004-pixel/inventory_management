package com.smartstock;

import com.smartstock.dto.ProductRequest;
import com.smartstock.dto.StockInRequest;
import com.smartstock.dto.StockOutRequest;
import com.smartstock.exception.BusinessRuleException;
import com.smartstock.exception.InsufficientStockException;
import com.smartstock.model.Product;
import com.smartstock.service.InventoryService;
import com.smartstock.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SmartStockApplicationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("Context Loads: Verifies Spring Boot and Oracle DB Connection")
    void contextLoads() {
        assertNotNull(productService);
        assertNotNull(inventoryService);
    }

    @Test
    @DisplayName("Test 1: Add new product and verify initial stock registration")
    void testAddNewProduct() {
        String uniqueSku = "TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductRequest req = new ProductRequest();
        req.setSku(uniqueSku);
        req.setProductName("Automated Test Device");
        req.setCategoryId(1L); // Electronics
        req.setPurchasePrice(new BigDecimal("25.00"));
        req.setSellingPrice(new BigDecimal("49.99"));
        req.setInitialQuantity(10);
        req.setMinStockLevel(3);
        req.setStatus("ACTIVE");

        Long newId = productService.createProduct(req);
        assertNotNull(newId);
        assertTrue(newId > 0);

        Product created = productService.getProductById(newId);
        assertEquals(uniqueSku, created.getSku());
        assertEquals(10, created.getQuantity());
        assertEquals("NORMAL", created.getStockStatus());
    }

    @Test
    @DisplayName("Test 2: Reject duplicate SKU on product creation")
    void testRejectDuplicateSku() {
        // KB-MECH-01 already exists from sample data
        ProductRequest req = new ProductRequest();
        req.setSku("KB-MECH-01");
        req.setProductName("Duplicate SKU Keyboard");
        req.setCategoryId(1L);
        req.setPurchasePrice(new BigDecimal("30.00"));
        req.setSellingPrice(new BigDecimal("60.00"));

        assertThrows(Exception.class, () -> productService.createProduct(req));
    }

    @Test
    @DisplayName("Test 3: Stock In increases product quantity and records movement")
    void testStockInReceiving() {
        // Product 1: KB-MECH-01
        int beforeQty = productService.getStockQuantity(1L);

        StockInRequest inReq = new StockInRequest();
        inReq.setProductId(1L);
        inReq.setQuantity(5);
        inReq.setUnitPrice(new BigDecimal("45.00"));
        inReq.setReferenceNumber("PO-TEST-" + System.currentTimeMillis());
        inReq.setRemarks("Automated test receiving");

        Integer newQty = inventoryService.stockIn(inReq);
        assertEquals(beforeQty + 5, newQty);

        int afterQty = productService.getStockQuantity(1L);
        assertEquals(beforeQty + 5, afterQty);
    }

    @Test
    @DisplayName("Test 4: Stock Out decreases product quantity")
    void testStockOutIssuance() {
        // Product 1: KB-MECH-01
        int beforeQty = productService.getStockQuantity(1L);
        assertTrue(beforeQty >= 5, "Product 1 must have at least 5 units");

        StockOutRequest outReq = new StockOutRequest();
        outReq.setProductId(1L);
        outReq.setQuantity(2);
        outReq.setReferenceNumber("SO-TEST-" + System.currentTimeMillis());
        outReq.setRemarks("Automated test issuance");

        Integer newQty = inventoryService.stockOut(outReq);
        assertEquals(beforeQty - 2, newQty);
    }

    @Test
    @DisplayName("Test 5: Stock Out is rejected when requested quantity exceeds available stock")
    void testStockOutInsufficientStock() {
        // Product 4: CHR-MESH-01 is OUT OF STOCK (qty 0)
        StockOutRequest outReq = new StockOutRequest();
        outReq.setProductId(4L);
        outReq.setQuantity(10);
        outReq.setReferenceNumber("SO-FAIL-" + System.currentTimeMillis());
        outReq.setRemarks("Should fail due to insufficient stock");

        assertThrows(InsufficientStockException.class, () -> inventoryService.stockOut(outReq));
    }

    @Test
    @DisplayName("Test 6: Reject zero or negative quantities")
    void testRejectZeroOrNegativeQuantity() {
        StockInRequest invalidIn = new StockInRequest();
        invalidIn.setProductId(1L);
        invalidIn.setQuantity(0);

        assertThrows(BusinessRuleException.class, () -> inventoryService.stockIn(invalidIn));

        StockOutRequest invalidOut = new StockOutRequest();
        invalidOut.setProductId(1L);
        invalidOut.setQuantity(-5);

        assertThrows(BusinessRuleException.class, () -> inventoryService.stockOut(invalidOut));
    }

    @Test
    @DisplayName("Test 7: Verify Low-Stock Alert detection via PL/SQL REF CURSOR")
    void testLowStockAlerts() {
        List<Product> alerts = productService.getLowStockAlerts();
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());

        for (Product p : alerts) {
            assertTrue(p.getQuantity() <= p.getMinStockLevel(),
                    "Product " + p.getProductName() + " qty (" + p.getQuantity() +
                    ") must be <= min level (" + p.getMinStockLevel() + ")");
        }
    }
}
