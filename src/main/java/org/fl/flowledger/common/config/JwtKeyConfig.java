package org.fl.flowledger.common.config;

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

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeyConfig {

    private final KeyPair keyPair;

    public JwtKeyConfig() {
        this.keyPair = generateKeyPair();
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

        return NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();
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