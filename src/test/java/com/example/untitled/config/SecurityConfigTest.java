package com.example.untitled.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Keycloak role mapping, which the MVC tests bypass by setting authorities directly.
 */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void mapsRealmRolesClientRolesAndScopes() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin")),
                "resource_access", Map.of(
                        "books-client", Map.of("roles", List.of("user")),
                        "no-roles-client", Map.of("roles", "not-a-list"),
                        "malformed-client", "not-a-map"),
                "scope", "openid email"));

        assertThat(authorities(jwt))
                .containsExactly("ROLE_ADMIN", "ROLE_USER", "SCOPE_openid", "SCOPE_email");
    }

    @Test
    void tokenWithoutRoleClaimsHasNoRoles() {
        assertThat(authorities(jwt(Map.of("realm_access", "not-a-map")))).isEmpty();
    }

    @Test
    void decoderUsesConfiguredKeycloakEndpoints() {
        ReflectionTestUtils.setField(config, "issuerUri", "http://localhost:8180/realms/books-realm");
        ReflectionTestUtils.setField(config, "jwkSetUri", "http://localhost:8180/realms/books-realm/protocol/openid-connect/certs");

        assertThat(config.jwtDecoder()).isInstanceOf(NimbusJwtDecoder.class);
    }

    private List<String> authorities(Jwt jwt) {
        return config.jwtGrantedAuthoritiesConverter().convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .claims(c -> c.putAll(claims))
                .build();
    }
}
