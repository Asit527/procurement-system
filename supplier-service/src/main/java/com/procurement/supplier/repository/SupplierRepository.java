package com.procurement.supplier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.procurement.supplier.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    // Check whether a supplier code already exists
    boolean existsBySupplierCode(String supplierCode);
    
 // Check whether another supplier already uses this code
    boolean existsBySupplierCodeAndIdNot(String supplierCode, Long id);
}