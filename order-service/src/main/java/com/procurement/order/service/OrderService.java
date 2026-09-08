package com.procurement.order.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.procurement.order.client.config.SupplierClient; // Current location
import com.procurement.order.dto.OrderRequest;
import com.procurement.order.dto.OrderResponse;
import com.procurement.order.entity.PurchaseOrder;
import com.procurement.order.repository.OrderRepository;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SupplierClient supplierClient;
    private final CircuitBreaker supplierCircuitBreaker;

    public OrderService(OrderRepository orderRepository,
                        SupplierClient supplierClient,
                        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.orderRepository = orderRepository;
        this.supplierClient = supplierClient;

        // Use the named circuit breaker configured in application.properties
        this.supplierCircuitBreaker =
                circuitBreakerRegistry.circuitBreaker("supplierLookup");
    }

    public OrderResponse createOrder(OrderRequest request) {
        try {
            // Protect only the remote supplier lookup
            supplierCircuitBreaker.executeSupplier(
                    () -> supplierClient.getSupplierById(request.supplierId()));

        } catch (FeignException.NotFound exception) {
            // A missing supplier is an invalid request, not a service outage
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Supplier not found with id: " + request.supplierId());

        } catch (CallNotPermittedException exception) {
            // The open circuit rejects calls immediately
            throw supplierUnavailable();

        } catch (FeignException exception) {
            // Handle connection failures and remote errors
            throw supplierUnavailable();
        }

        PurchaseOrder order = new PurchaseOrder(
                request.supplierId(),
                request.itemName(),
                request.quantity(),
                request.unitPrice());

        // Save only after successful supplier verification
        return toResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList(); // Convert entities into response DTOs
    }

    public OrderResponse getOrderById(Long id) {
        return toResponse(findOrder(id)); // Return the matching order
    }

    public OrderResponse cancelOrder(Long id) {
        PurchaseOrder order = findOrder(id);
        order.cancel(); // Mark the order as cancelled
        return toResponse(orderRepository.save(order));
    }

    private PurchaseOrder findOrder(Long id) {
        // Return 404 when the order does not exist
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found with id: " + id));
    }

    private ResponseStatusException supplierUnavailable() {
        // Preserve the existing 503 response
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to verify supplier. Please try again later.");
    }

    private OrderResponse toResponse(PurchaseOrder order) {
        // Map the entity into an API response
        return new OrderResponse(
                order.getId(),
                order.getSupplierId(),
                order.getItemName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus());
    }
}