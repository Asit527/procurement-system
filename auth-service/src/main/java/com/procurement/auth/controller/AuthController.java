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
    private final Map<String, LocalUser> users;
    private final String dummyPasswordHash;
    private final String issuer;

    public AuthController(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.username}") String adminUsername,
            @Value("${app.auth.password}") String adminPassword,
            @Value("${app.auth.viewer-password}") String viewerPassword,
            @Value("${app.auth.issuer}") String issuer) {

        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.passwordEncoder = passwordEncoder;
        this.issuer = issuer;

        // Permissions are assigned by the server, never by login input
        this.users = Map.of(
                adminUsername, new LocalUser(
                        passwordEncoder.encode(adminPassword),
                        "procurement.read procurement.write"),
                "viewer", new LocalUser(
                        passwordEncoder.encode(viewerPassword),
                        "procurement.read"));

        // Perform a password check even for unknown usernames
        this.dummyPasswordHash =
                passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LocalUser user = users.get(request.username());

        String hash = user != null ? user.passwordHash() : dummyPasswordHash;
        boolean passwordMatches =
                passwordEncoder.matches(request.password(), hash);

        if (user == null || !passwordMatches) {
            // Return the same error for invalid usernames and passwords
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(request.username())
                .audience(List.of("procurement-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900)) // Expire after 15 minutes
                .id(UUID.randomUUID().toString())
                .claim("scope", user.scope()) // Use this user's permissions
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)).getTokenValue();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store") // Avoid caching tokens
                .header("Pragma", "no-cache")
                .body(new LoginResponse(token, "Bearer", 900));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> publicKeys() {
        // Expose only the public verification key
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    private record LocalUser(String passwordHash, String scope) {
        // Local test account and its permissions
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
        // Token lifetime is expressed in seconds
    }
}