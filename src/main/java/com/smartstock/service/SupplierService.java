package com.smartstock.service;

import com.smartstock.dto.SupplierRequest;
import com.smartstock.exception.BusinessRuleException;
import com.smartstock.exception.ResourceNotFoundException;
import com.smartstock.model.Supplier;
import com.smartstock.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier with ID " + id + " not found."));
    }

    @Transactional
    public Long createSupplier(SupplierRequest request) {
        if (request.getSupplierName() == null || request.getSupplierName().trim().isEmpty()) {
            throw new BusinessRuleException("Supplier name is required.");
        }
        return supplierRepository.save(request);
    }

    @Transactional
    public void updateSupplier(Long id, SupplierRequest request) {
        getSupplierById(id);
        supplierRepository.update(id, request);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier s = getSupplierById(id);
        if (s.getAssociatedProductsCount() > 0) {
            throw new BusinessRuleException("Cannot delete supplier: " + s.getAssociatedProductsCount() + " products are associated with this supplier.");
        }
        supplierRepository.delete(id);
    }
}
