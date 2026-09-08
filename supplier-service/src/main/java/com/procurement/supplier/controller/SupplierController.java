package com.procurement.supplier.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.procurement.supplier.dto.SupplierRequest;
import com.procurement.supplier.dto.SupplierResponse;
import com.procurement.supplier.entity.Supplier;
import com.procurement.supplier.service.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService; // Constructor injection
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody SupplierRequest request) {

        Supplier supplier = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(supplier)); // Return response DTO
    }

    @GetMapping
    public List<SupplierResponse> getAllSuppliers() {
        return supplierService.getAllSuppliers().stream()
                .map(this::toResponse) // Convert each entity into a response DTO
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(
            @PathVariable Long id) {

        Supplier supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(toResponse(supplier)); // Return response DTO
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {

        Supplier supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(toResponse(supplier)); // Return updated response DTO
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build(); // Return 204 without a body
    }

    private SupplierResponse toResponse(Supplier supplier) {
        // Map database entity fields into the API response
        return new SupplierResponse(
                supplier.getId(),
                supplier.getSupplierCode(),
                supplier.getSupplierName());
    }
}