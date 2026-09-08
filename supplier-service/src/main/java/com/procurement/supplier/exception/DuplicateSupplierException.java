package com.procurement.supplier.exception;

public class DuplicateSupplierException extends RuntimeException {

    public DuplicateSupplierException(String supplierCode) {
        super("Supplier code already exists: " + supplierCode); // Set the error message
    }
}