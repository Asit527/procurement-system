package com.procurement.order.planning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Pure, deterministic planning engine. Does not reserve stock or create orders. */
public final class PurchasePlanner {
    public record Tier(int fromQuantity, BigDecimal unitPrice) {}
    public record Offer(String supplierCode, String partCode, int leadDays,
                        int minimumQuantity, int capacity, List<Tier> tiers) {}
    public record Demand(String partCode, int quantity, LocalDate neededBy) {}
    public record Request(LocalDate planningDate, List<Demand> demands, List<Offer> offers) {}
    public record Allocation(String supplierCode, int quantity, BigDecimal unitPrice,
                             BigDecimal cost, LocalDate arrivalDate, String reason) {}
    public record Alternative(String supplierCode, String reason) {}
    public record PartPlan(String partCode, int requested, int supplied, int shortage,
                           BigDecimal cost, List<Allocation> allocations,
                           List<Alternative> alternatives, String explanation) {}
    public record Plan(String status, BigDecimal totalCost, List<PartPlan> parts,
                       List<String> assumptions) {}

    private static final long INF = Long.MAX_VALUE / 4;

    public Plan plan(Request request) {
        validate(request);
        List<PartPlan> parts = new ArrayList<>();
        for (Demand demand : request.demands()) {
            parts.add(solve(request, demand));
        }
        BigDecimal total = parts.stream().map(PartPlan::cost)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        boolean complete = parts.stream().allMatch(p -> p.shortage() == 0);
        return new Plan(complete ? "FULFILLED" : "SHORTAGE", total, List.copyOf(parts), List.of(
                "Integer quantities; no overbuying; split orders allowed.",
                "Maximize on-time fulfilled quantity first, then minimize purchase cost.",
                "Each supplier/part capacity is available for this one plan, not shared across parts.",
                "One demand per part; calendar-day lead times measured from planningDate.",
                "All-units discounts: the qualifying tier price applies to the entire supplier lot.",
                "One common currency with two decimal places; no tax, freight or inventory holding cost.",
                "Offers are caller-provided snapshots; no supplier lookup, reservation or order placement.",
                "Equal-cost solutions use stable supplier-code order; no global capacity constraints."));
    }

    private PartPlan solve(Request request, Demand demand) {
        List<Offer> eligible = new ArrayList<>();
        List<Alternative> alternatives = new ArrayList<>();
        request.offers().stream().filter(o -> o.partCode().equals(demand.partCode()))
                .sorted(Comparator.comparing(Offer::supplierCode)).forEach(o -> {
                    LocalDate arrival = request.planningDate().plusDays(o.leadDays());
                    if (arrival.isAfter(demand.neededBy())) {
                        alternatives.add(new Alternative(o.supplierCode(),
                                "Rejected: arrival " + arrival + " is after deadline " + demand.neededBy()));
                    } else if (o.capacity() < o.minimumQuantity()) {
                        alternatives.add(new Alternative(o.supplierCode(), "Rejected: capacity is below minimum order quantity."));
                    } else if (o.minimumQuantity() > demand.quantity()) {
                        alternatives.add(new Alternative(o.supplierCode(), "Rejected: minimum order exceeds demand; overbuying is disabled."));
                    } else {
                        eligible.add(o);
                    }
                });

        int target = demand.quantity();
        long[] previous = new long[target + 1];
        Arrays.fill(previous, INF);
        previous[0] = 0;
        int[][] chosen = new int[eligible.size()][target + 1];
        for (int i = 0; i < eligible.size(); i++) {
            Offer offer = eligible.get(i);
            long[] next = previous.clone(); // Choosing zero preserves the previous suppliers' solution
            for (int quantity = offer.minimumQuantity(); quantity <= Math.min(target, offer.capacity()); quantity++) {
                long lotCost = cents(price(offer, quantity)) * quantity;
                for (int total = quantity; total <= target; total++) {
                    // Read only the previous row: each supplier is selected at most once
                    if (previous[total - quantity] != INF && previous[total - quantity] + lotCost < next[total]) {
                        next[total] = previous[total - quantity] + lotCost;
                        chosen[i][total] = quantity;
                    }
                }
            }
            previous = next;
        }
        // When full demand is impossible, maximize feasible supply before comparing costs
        int supplied = target;
        while (supplied > 0 && previous[supplied] == INF) supplied--;
        int remaining = supplied;
        List<Allocation> allocations = new ArrayList<>();
        Set<String> selected = new HashSet<>();
        for (int i = eligible.size() - 1; i >= 0; i--) {
            int quantity = chosen[i][remaining];
            if (quantity == 0) continue;
            Offer offer = eligible.get(i);
            BigDecimal unitPrice = price(offer, quantity).setScale(2);
            selected.add(offer.supplierCode());
            allocations.add(new Allocation(offer.supplierCode(), quantity, unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)),
                    request.planningDate().plusDays(offer.leadDays()),
                    "Selected in the minimum-cost feasible allocation: MOQ " + offer.minimumQuantity()
                            + ", capacity " + offer.capacity() + ", all-units price " + unitPrice
                            + " at quantity " + quantity + "; arrives by the deadline."));
            remaining -= quantity;
        }
        allocations.sort(Comparator.comparing(Allocation::supplierCode));
        for (Offer offer : eligible) {
            if (!selected.contains(offer.supplierCode())) {
                alternatives.add(new Alternative(offer.supplierCode(),
                        "Eligible but not selected: no lower-cost allocation for the chosen fulfillment quantity; ties retain earlier suppliers."));
            }
        }
        alternatives.sort(Comparator.comparing(Alternative::supplierCode));
        return new PartPlan(demand.partCode(), target, supplied, target - supplied,
                BigDecimal.valueOf(previous[supplied], 2), List.copyOf(allocations), List.copyOf(alternatives),
                supplied == target ? "Full demand met at the minimum purchase cost under the stated assumptions."
                        : "Full demand is infeasible. This is the maximum on-time quantity possible without overbuying, at its minimum cost.");
    }

    private BigDecimal price(Offer offer, int quantity) {
        return offer.tiers().stream().filter(t -> t.fromQuantity() <= quantity)
                .max(Comparator.comparingInt(Tier::fromQuantity)).orElseThrow().unitPrice();
    }

    private long cents(BigDecimal price) { return price.movePointRight(2).longValueExact(); }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private void validate(Request r) {
        require(r != null && r.planningDate() != null, "planningDate is required");
        require(r.demands() != null && !r.demands().isEmpty() && r.demands().size() <= 20,
                "Provide 1 to 20 demands");
        require(r.offers() != null && r.offers().size() <= 400, "Provide at most 400 offers");
        Set<String> parts = new HashSet<>();
        for (Demand d : r.demands()) {
            require(d != null && validCode(d.partCode()), "Each demand needs a partCode (1-64 characters)");
            require(parts.add(d.partCode()), "Duplicate demand part: " + d.partCode());
            require(d.quantity() > 0 && d.quantity() <= 500, "Demand quantity must be 1-500");
            require(d.neededBy() != null && !d.neededBy().isBefore(r.planningDate()), "neededBy must be on or after planningDate");
        }
        Set<List<String>> pairs = new HashSet<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Offer o : r.offers()) {
            require(o != null && validCode(o.supplierCode()) && validCode(o.partCode()), "Offer codes must contain 1-64 nonblank characters");
            require(parts.contains(o.partCode()), "Offer refers to a part not present in demands: " + o.partCode());
            require(pairs.add(List.of(o.supplierCode(), o.partCode())), "Duplicate supplier/part offer");
            require(counts.merge(o.partCode(), 1, Integer::sum) <= 20, "At most 20 offers per part");
            require(o.leadDays() >= 0 && o.leadDays() <= 3650, "leadDays must be 0-3650");
            require(o.minimumQuantity() > 0 && o.minimumQuantity() <= 1000000, "minimumQuantity must be 1-1000000");
            require(o.capacity() >= 0 && o.capacity() <= 1000000, "capacity must be 0-1000000");
            require(o.tiers() != null && !o.tiers().isEmpty() && o.tiers().size() <= 20, "Provide 1-20 price tiers");
            Set<Integer> thresholds = new HashSet<>();
            for (Tier t : o.tiers()) {
                require(t != null && t.fromQuantity() > 0 && t.fromQuantity() <= 1000000, "Invalid tier threshold");
                require(thresholds.add(t.fromQuantity()), "Duplicate price tier threshold");
                require(t.unitPrice() != null && t.unitPrice().signum() > 0
                        && t.unitPrice().compareTo(new BigDecimal("1000000")) <= 0, "Price must be positive and at most 1000000");
                try { cents(t.unitPrice()); } catch (ArithmeticException e) {
                    throw new IllegalArgumentException("Price must have at most two nonzero decimal places");
                }
            }
            require(thresholds.contains(1), "Each offer needs a base tier fromQuantity=1");
            List<Tier> sorted = o.tiers().stream().sorted(Comparator.comparingInt(Tier::fromQuantity)).toList();
            for (int i = 1; i < sorted.size(); i++) {
                require(sorted.get(i).unitPrice().compareTo(sorted.get(i - 1).unitPrice()) <= 0,
                        "Bulk tier prices must not increase");
            }
        }
    }

    private boolean validCode(String code) {
        return code != null && !code.isBlank() && code.length() <= 64 && code.equals(code.trim());
    }
}
