package org.fl.flowledger.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    private Jwt jwtWithRole(String role) {
        return Jwt.withTokenValue("dummy-token")
                .header("alg", "RS256")
                .claim("sub", "42")
                .claim("role", role)
                .build();
    }

    @Test
    void convert_addsExactlyOneRolePrefix_forBareRoleClaim() {
        AbstractAuthenticationToken token = converter.convert(jwtWithRole("USER"));

        assertEquals(1, token.getAuthorities().size());

        String authority = token.getAuthorities().iterator().next().getAuthority();
        assertEquals("ROLE_USER", authority,
                "Expected a single ROLE_ prefix - got '" + authority + "'. " +
                "If this is ROLE_ROLE_USER, something upstream is putting an " +
                "already-prefixed authority into the JWT's role claim.");
    }

    @Test
    void convert_worksForAdminRoleToo() {
        AbstractAuthenticationToken token = converter.convert(jwtWithRole("ADMIN"));

        GrantedAuthority authority = token.getAuthorities().iterator().next();
        assertEquals("ROLE_ADMIN", authority.getAuthority());
    }

    @Test
    void convert_neverProducesADoublePrefixedAuthority() {
        AbstractAuthenticationToken token = converter.convert(jwtWithRole("USER"));

        String authority = token.getAuthorities().iterator().next().getAuthority();
        assertFalse(authority.startsWith("ROLE_ROLE_"));
    }

    @Test
    void convert_usesJwtSubjectAsThePrincipalName() {
        AbstractAuthenticationToken token = converter.convert(jwtWithRole("USER"));

        assertEquals("42", token.getName());
    }
}
