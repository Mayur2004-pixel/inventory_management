package com.smartstock.model;

import java.time.LocalDateTime;

public class Supplier {
    private Long supplierId;
    private String supplierName;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private int associatedProductsCount;

    public Supplier() {}

    public Supplier(Long supplierId, String supplierName, String phone, String email, String address, LocalDateTime createdAt) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getAssociatedProductsCount() {
        return associatedProductsCount;
    }

    public void setAssociatedProductsCount(int associatedProductsCount) {
        this.associatedProductsCount = associatedProductsCount;
    }
}
