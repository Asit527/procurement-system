# Purchasing planner — assignment decisions

## Problem and objective

For each requested part, choose integer quantities from supplier offers. A selected lot must meet the supplier's minimum quantity, must not exceed available capacity, and must arrive by the demand deadline. The applicable bulk tier sets the unit price for the entire lot.

The objective is lexicographic: **maximize on-time quantity supplied without exceeding demand, then minimize total purchase cost**. If full demand is feasible, this gives the exact minimum-cost plan. Otherwise, return the best partial plan and an explicit shortage. A shortage is a valid 200 response with status SHORTAGE, not a silently successful full plan.

## API contract

POST `/api/orders/purchase-plans` accepts:

- `planningDate`: ISO date from which lead times are measured. Historical dates are allowed for reproducible scenarios.
- `demands`: partCode, positive integer quantity, and neededBy date.
- `offers`: supplierCode, partCode, leadDays, minimumQuantity, capacity, and price tiers.
- `tiers`: fromQuantity threshold and unitPrice. A base tier starting at 1 is required. Tiers may be supplied in any order; duplicate thresholds and increasing prices are rejected.

The caller submits the offers and requirements together as an immutable planning snapshot. Codes are exact, case-sensitive identifiers; duplicate demand parts and duplicate supplier/part offers are rejected. Offers for unrelated parts are rejected rather than silently ignored. No supplier-service registration is needed for these offer codes.

Response fields:

| Field | Meaning |
| --- | --- |
| status | FULFILLED only if every part is fully supplied; otherwise SHORTAGE |
| totalCost | Sum of selected lots across all parts |
| parts[].supplied / shortage | Feasible allocation and uncovered demand |
| allocations | Supplier code, chosen quantity, unit price, cost, arrival date, selection reason |
| alternatives | Late, MOQ/capacity-ineligible, or eligible-but-unused offers and reasons |
| explanation / assumptions | Objective and constraints used to derive the result |

Prices use decimal currency values converted to integer cents during optimization. No floating-point cost comparisons are used.

## Exact algorithm

Process each part independently. Filter out offers that arrive late or cannot satisfy their minimum quantity within demand/capacity. Sort eligible suppliers by code for reproducible tie handling.

For each supplier, enumerate quantity zero and every allowed quantity from MOQ through min(capacity, demand). A dynamic-programming state stores the lowest cost for exactly q units using suppliers processed so far:

`next[q] = min(previous[q - x] + cost(supplier, x))`

Every update reads the previous supplier row, so the same supplier cannot be selected twice and its capacity cannot be reused. Store chosen quantities for backtracking. Inspect the demand state first; if unreachable, select the largest reachable quantity. Backtrack to recover supplier allocations.

All-units discounts can make greedy allocation incorrect. In the demo, A can supply 60 at 8/unit, but filling the remainder from B at 12/unit costs 960. Buying all 100 from B activates its 9/unit tier and costs 900. The DP compares both and selects B.

Time per part is O(S × Q²); storage is O(S × Q) for backtracking plus O(Q) cost rows. Limits of 20 parts, 20 offers per part, and 500 demanded units per part bound the computation. This is an exact bounded solution, not an unbounded production optimizer. Stable code ordering resolves equal-cost choices; no business preference or claim of supplier fairness is implied.

## Explicit assumptions

1. Parts are independent. Capacity is per supplier/part for this request, not a shared supplier-wide limit.
2. One demand line/deadline per part. The client must consolidate compatible requirements; repeated parts with different deadlines need a future time-phased model.
3. Quantities are indivisible integer units. Split sourcing is allowed, overbuying is not.
4. MOQ applies per supplier/part lot. No pack-size multiples are assumed.
5. Lead times are calendar days; arrival on the deadline is allowed. No holidays or transit uncertainty.
6. Discounts are all-units, not incremental tiers, and apply per supplier/part lot.
7. Prices use a common currency and at most two nonzero decimal places. Freight, tax, inventory cost, budgets, quality scores, and supplier preferences are excluded.
8. Available capacities are supplied as a snapshot. Planning does not consume them across concurrent requests.
9. Purchasing decisions are proposals for review, not automatic procurement commitments.

## Validation and tests

Tests cover a non-greedy discount decision, split capacity, MOQ infeasibility, deadline inclusion/exclusion, no offers, multiple parts, deterministic tie handling, malformed input, and 200 small randomized cases checked by an independent brute-force enumerator. API contract tests verify the example JSON and a structured 400 validation response. The bounded values avoid integer overflow in cent-based totals.

## Deliberate omissions and why

- **Offer/demand database CRUD and saved plan history:** the request captures the full reproducible snapshot; omitted to prioritize the assignment's decision logic within the deadline.
- **Capacity reservation and order conversion:** require transactional/concurrency rules and purchase-team approval; the existing manual order API remains separate.
- **Shared capacities, multiple deadlines, shipping and budget constraints:** would couple parts or periods and need a more general optimization model.
- **Unbounded quantities:** the exact DP is intentionally bounded; larger instances would justify MILP or another solver.
- **Counterfactual reports for every possible alternative:** the API explains constraint rejections and the optimal objective, but does not enumerate all competing plans.

The existing six-service infrastructure is supporting work. The assignment's main implementation is `PurchasePlanner.java`; it can be understood and tested independently of Eureka, MySQL, JWT issuance, or GitHub.
