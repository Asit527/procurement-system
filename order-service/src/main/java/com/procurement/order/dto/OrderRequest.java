package com.procurement.order.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record OrderRequest(
        @NotNull(message = "Supplier ID is required")
        @Positive(message = "Supplier ID must be positive")
        Long supplierId,

        @NotBlank(message = "Item name is required")
        @Size(max = 150, message = "Item name cannot exceed 150 characters")
        String itemName,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "Invalid unit price")
        BigDecimal unitPrice
) {
    // Validated input for creating an order
}