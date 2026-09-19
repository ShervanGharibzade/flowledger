package org.fl.flowledger.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.fl.flowledger.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Provides the RSA key pair used to sign and verify access tokens.
 *
 * If app.jwt.private-key / app.jwt.public-key are configured (PEM strings,
 * typically injected from a secret store via JWT_PRIVATE_KEY / JWT_PUBLIC_KEY
 * env vars), those are used, so the same key survives restarts and is shared
 * across every instance behind a load balancer.
 *
 * If they are not set, a key pair is generated in memory as a local-dev-only
 * fallback. This is NOT suitable for production: tokens won't survive a
 * restart, and a multi-instance deployment will have each instance signing
 * with a different key, causing intermittent token-verification failures.
 */
@Slf4j
@Configuration
public class JwtKeyConfig {

    @Value("${app.jwt.private-key:}")
    private String privateKeyPem;

    @Value("${app.jwt.public-key:}")
    private String publicKeyPem;

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        if (!privateKeyPem.isBlank() && !publicKeyPem.isBlank()) {
            this.keyPair = loadKeyPair(privateKeyPem, publicKeyPem);
            log.info("Loaded configured RSA key pair for JWT signing.");
        } else {
            log.warn(
                    "app.jwt.private-key / app.jwt.public-key are not set. Generating an " +
                    "EPHEMERAL RSA key pair for JWT signing. Tokens will not survive an " +
                    "application restart and will not be valid across multiple instances. " +
                    "This is only acceptable for local development - set those properties " +
                    "(e.g. via JWT_PRIVATE_KEY / JWT_PUBLIC_KEY) for any shared or " +
                    "production environment."
            );
            this.keyPair = generateKeyPair();
        }
    }

    @Bean
    public JwtEncoder jwtEncoder() {

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        JWK jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        ImmutableJWKSet<SecurityContext> jwkSet =
                new ImmutableJWKSet<>(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();

        // Default validation (NimbusJwtDecoder) already checks exp/nbf with
        // zero clock skew. We layer on:
        //  - an issuer check, so a token from another service/environment
        //    that happened to be signed with this same key pair is rejected
        //  - 30s of clock-skew tolerance, so slightly-out-of-sync clocks
        //    between instances don't cause spurious "expired" rejections
        //    right at the boundary
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                List.of(
                        new JwtIssuerValidator(JwtService.ISSUER),
                        new JwtTimestampValidator(Duration.ofSeconds(30))
                )
        );

        decoder.setJwtValidator(validator);

        return decoder;
    }

    private KeyPair loadKeyPair(String privatePem, String publicPem) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(privatePem))
            );

            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(decodePem(publicPem))
            );

            return new KeyPair(publicKey, privateKey);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load configured RSA key pair for JWT signing " +
                    "(app.jwt.private-key / app.jwt.public-key). Expected PEM-encoded " +
                    "PKCS8 private key and X.509 public key.", e
            );
        }
    }

    private byte[] decodePem(String pem) {
        String cleaned = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(cleaned);
    }

    private KeyPair generateKeyPair() {
        try {
            var generator =
                    java.security.KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate RSA key pair", e
            );
        }
    }
}
