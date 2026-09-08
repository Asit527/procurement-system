package com.procurement.supplier.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.procurement.supplier.dto.SupplierRequest;
import com.procurement.supplier.entity.Supplier;
import com.procurement.supplier.exception.DuplicateSupplierException;
import com.procurement.supplier.exception.SupplierNotFoundException;
import com.procurement.supplier.repository.SupplierRepository;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository; // Constructor injection
    }

    public Supplier createSupplier(SupplierRequest request) {
        // Reject a code already used by another supplier
        if (supplierRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new DuplicateSupplierException(request.getSupplierCode());
        }

        Supplier supplier = new Supplier(); // Convert request DTO into an entity
        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setSupplierName(request.getSupplierName());

        return supplierRepository.save(supplier); // Save the new supplier
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll(); // Fetch all suppliers
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id)); // Handle missing ID
    }

    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier existingSupplier = getSupplierById(id); // Verify supplier exists

        // Check for duplicate codes while excluding this supplier
        if (supplierRepository.existsBySupplierCodeAndIdNot(
                request.getSupplierCode(), id)) {
            throw new DuplicateSupplierException(request.getSupplierCode());
        }

        existingSupplier.setSupplierCode(request.getSupplierCode()); // Update code
        existingSupplier.setSupplierName(request.getSupplierName()); // Update name

        return supplierRepository.save(existingSupplier); // Save changes
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id); // Verify supplier exists
        supplierRepository.delete(supplier); // Delete supplier
    }
}