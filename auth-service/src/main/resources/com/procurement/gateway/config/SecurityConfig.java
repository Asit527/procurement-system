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
                // Bearer tokens are supplied explicitly; no cookie authentication
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login and health checks remain public
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // Reading requires the read scope
                        .requestMatchers(HttpMethod.GET,
                                "/api/suppliers/**", "/api/orders/**")
                        .hasAuthority("SCOPE_procurement.read")

                        // Other operations require the write scope
                        .requestMatchers("/api/suppliers/**", "/api/orders/**")
                        .hasAuthority("SCOPE_procurement.write")

                        .anyRequest().denyAll())
                // Use Spring Security's JWT validation
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}package com.procurement.gateway.config;

public class SecurityConfig {

}
