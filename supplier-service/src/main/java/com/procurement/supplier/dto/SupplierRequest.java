package com.procurement.supplier.dto;

import jakarta.validation.constraints.NotBlank;

public class SupplierRequest {

    @NotBlank(message = "Supplier code is required") // Reject blank codes
    private String supplierCode;

    @NotBlank(message = "Supplier name is required") // Reject blank names
    private String supplierName;

    public String getSupplierCode() {
        return supplierCode; // Read supplier code
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode; // Set supplier code
    }

    public String getSupplierName() {
        return supplierName; // Read supplier name
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName; // Set supplier name
    }
}