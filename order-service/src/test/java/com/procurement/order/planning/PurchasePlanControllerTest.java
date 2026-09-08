package com.procurement.order.planning;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import com.procurement.order.exception.GlobalExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchasePlanControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PurchasePlanController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test void exampleReturnsExplainedPlan() throws Exception {
        // Check JSON binding and response shape without MySQL or Eureka
        String json = Files.readString(Path.of("../examples/purchase-plan.json"));
        mvc.perform(post("/api/orders/purchase-plans").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.totalCost").value(900.0))
                .andExpect(jsonPath("$.parts[0].allocations[0].supplierCode").value("SUP-B"))
                .andExpect(jsonPath("$.parts[0].alternatives[1].supplierCode").value("SUP-LATE"));
    }
    @Test void invalidDemandReturnsClearBadRequest() throws Exception {
        mvc.perform(post("/api/orders/purchase-plans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planningDate\":\"2026-09-08\",\"demands\":[],\"offers\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provide 1 to 20 demands"));
    }
}
