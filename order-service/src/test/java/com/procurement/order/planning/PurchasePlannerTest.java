package com.procurement.order.planning;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.procurement.order.planning.PurchasePlanner.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

class PurchasePlannerTest {
    private final PurchasePlanner planner = new PurchasePlanner();
    private final LocalDate start = LocalDate.of(2026, 9, 8);
    private Tier tier(int q, String price) { return new Tier(q, new BigDecimal(price)); }
    private Offer offer(String code, int min, int cap, Tier... tiers) {
        return new Offer(code, "P", 1, min, cap, List.of(tiers));
    }
    private Request request(int quantity, Offer... offers) {
        return new Request(start, List.of(new Demand("P", quantity, start.plusDays(7))), List.of(offers));
    }
    @Test void discountsCanBeatGreedySplit() {
        // A has cheaper marginal units, but B's full-lot discount wins globally
        Plan p = planner.plan(request(100,
                offer("A", 20, 60, tier(1,"10"), tier(50,"8")),
                offer("B", 10, 100, tier(1,"12"), tier(80,"9"))));
        assertEquals(new BigDecimal("900.00"), p.totalCost());
        assertEquals("B", p.parts().get(0).allocations().get(0).supplierCode());
    }
    @Test void splitsRespectCapacityAndMinimums() {
        Plan p = planner.plan(request(10, offer("A",3,6,tier(1,"2")), offer("B",4,8,tier(1,"3"))));
        assertEquals(new BigDecimal("24.00"), p.totalCost());
        assertEquals(List.of(6,4), p.parts().get(0).allocations().stream().map(Allocation::quantity).toList());
    }
    @Test void lateSupplierIsExcludedButDeadlineIsInclusive() {
        Offer late = new Offer("LATE","P",8,1,100,List.of(tier(1,"1")));
        Offer onTime = new Offer("OK","P",7,1,100,List.of(tier(1,"2")));
        Plan p = planner.plan(request(5, late, onTime));
        assertEquals(new BigDecimal("10.00"),p.totalCost());
        assertTrue(p.parts().get(0).alternatives().get(0).reason().contains("after deadline"));
    }
    @Test void reportsMaximumFeasibleShortageWithoutOverbuying() {
        Plan p = planner.plan(request(10,offer("A",6,6,tier(1,"2")),offer("B",6,6,tier(1,"1"))));
        assertEquals("SHORTAGE",p.status());
        assertEquals(4,p.parts().get(0).shortage());
        assertEquals(new BigDecimal("6.00"),p.totalCost());
    }
    @Test void noOffersProducesFullShortage() {
        assertEquals(5,planner.plan(request(5)).parts().get(0).shortage());
    }
    @Test void rejectsMalformedBusinessInput() {
        assertThrows(IllegalArgumentException.class, () -> planner.plan(request(501)));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(request(2,offer("A",1,2,tier(2,"1")))));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(request(2,offer("A",1,2,tier(1,"1.001")))));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(request(2,offer("A",1,2,tier(1,"1"),tier(2,"2")))));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(request(2,offer("A",1,2,tier(1,"1")),offer("A",1,2,tier(1,"2")))));
    }
    @Test void multiplePartsAndDeterministicTie() {
        Offer a=offer("A",1,10,tier(1,"1")); Offer b=offer("B",1,10,tier(1,"1"));
        assertEquals(planner.plan(request(5,a,b)),planner.plan(request(5,b,a)));
        Request r=new Request(start,List.of(new Demand("P",2,start),new Demand("Q",3,start.plusDays(2))),
                List.of(new Offer("A","Q",1,1,3,List.of(tier(1,"2")))));
        Plan p=planner.plan(r);
        assertEquals(2,p.parts().get(0).shortage());
        assertEquals(new BigDecimal("6.00"),p.totalCost());
    }
    @Test void agreesWithExhaustiveEnumerationOnTwoHundredSmallCases() {
        Random random = new Random(42);
        for(int trial=0; trial<200; trial++) {
            int demand=1+random.nextInt(12);
            List<Offer> offers=new ArrayList<>();
            for(int i=0;i<3;i++) {
                int base=2+random.nextInt(8);
                offers.add(offer("S"+i,1+random.nextInt(4),random.nextInt(9),
                        tier(1,String.valueOf(base)),tier(5,String.valueOf(base-1))));
            }
            long[] best={-1,Long.MAX_VALUE};
            exhaustive(offers,0,0,0,demand,best);
            Plan p=planner.plan(request(demand,offers.toArray(Offer[]::new)));
            assertEquals(best[0],p.parts().get(0).supplied(),"trial "+trial);
            assertEquals(best[1],p.totalCost().longValueExact(),"trial "+trial);
        }
    }
    private void exhaustive(List<Offer> offers,int i,int supplied,long cost,int demand,long[] best) {
        // Independent brute-force oracle enumerates every supplier lot combination
        if(i==offers.size()) {
            if(supplied>best[0] || supplied==best[0] && cost<best[1]) {best[0]=supplied;best[1]=cost;}
            return;
        }
        Offer o=offers.get(i);
        exhaustive(offers,i+1,supplied,cost,demand,best);
        for(int q=o.minimumQuantity();q<=o.capacity() && supplied+q<=demand;q++) {
            long price=o.tiers().get(q>=5 ? 1 : 0).unitPrice().longValueExact();
            exhaustive(offers,i+1,supplied+q,cost+q*price,demand,best);
        }
    }
}
