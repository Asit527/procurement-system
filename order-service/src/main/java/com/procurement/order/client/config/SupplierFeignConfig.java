package com.procurement.order.client.config; // Match the destination package

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import feign.RequestInterceptor;

// Intentionally no @Configuration: applied only to SupplierClient
public class SupplierFeignConfig {

    @Bean
    public RequestInterceptor supplierTokenInterceptor() {
        return template -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwt) {
                // Forward the authenticated caller's JWT to supplier-service
                template.header(
                        "Authorization",
                        "Bearer " + jwt.getToken().getTokenValue());
            }
        };
    }
}