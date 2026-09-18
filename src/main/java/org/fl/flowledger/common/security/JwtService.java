package org.fl.flowledger.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String ISSUER = "https://flow-ledger.internal";

    private static final long ACCESS_TOKEN_TTL_SECONDS = 900; // 15 minutes

    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(String id, String role) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(id)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS))
                .claim("role", role)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(() -> "RS256").build(),
                        claims
                )
        ).getTokenValue();
    }
}
