package com.smartstock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockOutRequest {
    @NotNull(message = "Product selection is required")
    private Long productId;

    @NotNull(message = "Quantity issued is required")
    @Min(value = 1, message = "Quantity issued must be at least 1")
    private Integer quantity;

    private String referenceNumber;

    private String remarks;

    public StockOutRequest() {}

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
