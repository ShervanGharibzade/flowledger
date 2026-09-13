package org.fl.flowledger.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String role = jwt.getClaimAsString("role");

        return new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                ),
                Objects.requireNonNull(jwt.getSubject())
        );
    }
}