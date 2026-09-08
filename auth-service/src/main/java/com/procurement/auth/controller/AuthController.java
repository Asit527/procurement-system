package com.procurement.auth.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class AuthController {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String passwordHash;
    private final String issuer;

    public AuthController(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.username}") String username,
            @Value("${app.auth.password}") String password,
            @Value("${app.auth.issuer}") String issuer) {

        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.passwordHash = passwordEncoder.encode(password); // Keep a hash
        this.issuer = issuer;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        // Check the password even when the username is incorrect
        boolean passwordMatches =
                passwordEncoder.matches(request.password(), passwordHash);

        if (!username.equals(request.username()) || !passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .audience(List.of("procurement-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900)) // Token expires in 15 minutes
                .id(UUID.randomUUID().toString())
                .claim("scope", "procurement.read procurement.write")
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)).getTokenValue();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store") // Avoid caching access tokens
                .header("Pragma", "no-cache")
                .body(new LoginResponse(token, "Bearer", 900));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> publicKeys() {
        // Publish only the public key; never expose the private key
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    public record LoginRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 64) String password) {
        // Validated login input
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn) {
        // Login response; expiresIn is in seconds
    }
}