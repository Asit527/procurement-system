package com.procurement.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.procurement.order.entity.PurchaseOrder;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    // Spring Data supplies standard database operations
}