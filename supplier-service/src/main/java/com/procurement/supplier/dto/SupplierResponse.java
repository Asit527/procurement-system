package com.procurement.supplier.dto;

public class SupplierResponse {

    private final Long id;
    private final String supplierCode;
    private final String supplierName;

    // Constructor used by the controller's toResponse() method
    public SupplierResponse(Long id, String supplierCode, String supplierName) {
        this.id = id;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
    }

    public Long getId() {
        return id; // Expose ID in JSON
    }

    public String getSupplierCode() {
        return supplierCode; // Expose code in JSON
    }

    public String getSupplierName() {
        return supplierName; // Expose name in JSON
    }
}