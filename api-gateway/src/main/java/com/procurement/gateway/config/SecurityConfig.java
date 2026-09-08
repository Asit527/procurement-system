package com.procurement.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // Use bearer tokens without cookie-based authentication
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow login and health checks
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // Require read permission for GET requests
                        .requestMatchers(HttpMethod.GET,
                                "/api/suppliers/**", "/api/orders/**")
                        .hasAuthority("SCOPE_procurement.read")

                        // Require write permission for other API operations
                        .requestMatchers("/api/suppliers/**", "/api/orders/**")
                        .hasAuthority("SCOPE_procurement.write")

                        .anyRequest().denyAll())
                // Validate JWT access tokens
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}