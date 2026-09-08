package com.procurement.order.planning;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders/purchase-plans")
public class PurchasePlanController {
    private final PurchasePlanner planner = new PurchasePlanner();

    @PostMapping
    public PurchasePlanner.Plan create(@RequestBody PurchasePlanner.Request request) {
        try {
            return planner.plan(request); // Return a proposal; never place orders automatically
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
