package com.procurement.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.procurement.order.dto.SupplierResponse;

@FeignClient(name = "supplier-service") // Find supplier-service through Eureka
public interface SupplierClient {

    @GetMapping("/api/suppliers/{id}")
    SupplierResponse getSupplierById(@PathVariable("id") Long id);
}