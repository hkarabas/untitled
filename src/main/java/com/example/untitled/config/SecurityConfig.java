package com.example.untitled.config;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Keycloak OAuth2 integration.
 * The API is a stateless resource server: clients send a Keycloak-issued JWT as a bearer token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    private static final String ROLES_CLAIM = "roles";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/api/public/**",
            "/api/health",
            "/",
            "/health",
            "/info",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // Safe: the API is stateless and authenticates only with a bearer token in the Authorization
                // header, never with cookies, so a cross-site request cannot carry the user's credentials
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 (see comment above)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/books/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            // Realm roles: realm_access.roles
            Stream<GrantedAuthority> realmRoles = roleAuthorities(jwt.getClaim("realm_access"));

            // Client roles: resource_access.<client>.roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Stream<GrantedAuthority> clientRoles = resourceAccess == null
                    ? Stream.empty()
                    : resourceAccess.values().stream().flatMap(SecurityConfig::roleAuthorities);

            // Scope-based authorities (SCOPE_...); the converter never returns null
            Stream<GrantedAuthority> scopes = scopeConverter.convert(jwt).stream();

            return Stream.concat(Stream.concat(realmRoles, clientRoles), scopes).toList();
        };
    }

    /**
     * {@code ROLE_} authorities from a claim shaped like {@code {"roles": ["user", ...]}};
     * any other shape yields none.
     */
    private static Stream<GrantedAuthority> roleAuthorities(Object access) {
        if (!(access instanceof Map<?, ?> accessMap) || !(accessMap.get(ROLES_CLAIM) instanceof Collection<?> roles)) {
            return Stream.empty();
        }
        // Locale.ROOT: in a Turkish locale "admin".toUpperCase() is "ADMİN", which would never match hasRole("ADMIN")
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase(Locale.ROOT)));
    }



    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
