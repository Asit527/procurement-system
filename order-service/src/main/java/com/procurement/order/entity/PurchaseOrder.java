package com.procurement.order.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "purchase_orders") // Database table name
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long supplierId; // Supplier owned by supplier-service

    @Column(nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 20)
    private String status;

    protected PurchaseOrder() {
        // Required by JPA
    }

    public PurchaseOrder(Long supplierId, String itemName,
                         Integer quantity, BigDecimal unitPrice) {
        this.supplierId = supplierId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = "CREATED"; // Initial order status
    }

    public Long getId() {
        return id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)); // Calculate total
    }

    public void cancel() {
        this.status = "CANCELLED"; // Mark the order as cancelled
    }
}