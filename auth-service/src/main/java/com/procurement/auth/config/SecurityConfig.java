package com.procurement.auth.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // Stateless API without cookie-based authentication
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/.well-known/jwks.json",
                                "/actuator/health").permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Hash the test account password
    }

    @Bean
    public RSAKey rsaKey() throws Exception {
        // Read keys from your home folder, outside Git
        Path folder = Path.of(
                System.getProperty("user.home"), ".procurement", "keys");

        byte[] privateBytes = readPem(
                folder.resolve("auth-private.pem"), "PRIVATE KEY");
        byte[] publicBytes = readPem(
                folder.resolve("auth-public.pem"), "PUBLIC KEY");

        KeyFactory factory = KeyFactory.getInstance("RSA");

        RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                new PKCS8EncodedKeySpec(privateBytes));

        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new X509EncodedKeySpec(publicBytes));

        // Stop startup if the files contain different key pairs
        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException("RSA public and private keys do not match");
        }

        // Derive a stable key ID from the public key
        String keyId = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded()));

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();
    }

    private byte[] readPem(Path path, String type) throws Exception {
        // Remove PEM headers and decode the key bytes
        String contents = Files.readString(path, StandardCharsets.US_ASCII)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(contents);
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        // Sign tokens with the persistent private key
        return new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }
}