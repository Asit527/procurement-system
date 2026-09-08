package com.procurement.supplier.exception;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(Long id) {
        super("Supplier not found with id: " + id); // Set the error message
    }
}