package com.procurement.order.dto;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long supplierId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status
) {
    // Data returned by order APIs
}