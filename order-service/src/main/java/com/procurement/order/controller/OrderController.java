package com.procurement.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.procurement.order.dto.OrderRequest;
import com.procurement.order.dto.OrderResponse;
import com.procurement.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService; // Constructor injection
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request)); // Return 201
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders(); // Return all orders
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable("id") Long id) {
        return orderService.getOrderById(id); // Return one order
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable("id") Long id) {
        return orderService.cancelOrder(id); // Cancel the order
    }
}