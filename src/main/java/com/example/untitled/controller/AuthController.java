package com.example.untitled.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secured endpoints demonstrating Keycloak authentication
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Keycloak secured endpoints")
public class AuthController {

    @GetMapping("/public/info")
    @Operation(summary = "Public endpoint - no authentication required")
    public Map<String, Object> publicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "accessible to everyone");
        return response;
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get user profile",
            description = "Requires USER or ADMIN role",
            security = @SecurityRequirement(name = "oauth2", scopes = {"openid", "profile", "email"})
    )
    public Map<String, Object> userProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", jwt.getClaimAsString("preferred_username"));
        profile.put("email", jwt.getClaimAsString("email"));
        profile.put("name", jwt.getClaimAsString("name"));
        profile.put("given_name", jwt.getClaimAsString("given_name"));
        profile.put("family_name", jwt.getClaimAsString("family_name"));

        // Extract roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            profile.put("roles", realmAccess.get("roles"));
        }

        profile.put("subject", jwt.getSubject());
        profile.put("issuer", jwt.getIssuer());

        return profile;
    }

    @GetMapping("/user/claims")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get all JWT claims",
            description = "Returns all claims from the JWT token. Requires USER or ADMIN role",
            security = @SecurityRequirement(name = "oauth2", scopes = {"openid", "profile", "email"})
    )
    public Map<String, Object> getAllClaims(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin dashboard",
            description = "Only accessible to users with ADMIN role",
            security = @SecurityRequirement(name = "oauth2", scopes = {"openid", "profile", "email"})
    )
    public Map<String, Object> adminDashboard(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("message", "Welcome to Admin Dashboard");
        dashboard.put("admin", jwt.getClaimAsString("preferred_username"));
        dashboard.put("access_level", "ADMIN");
        dashboard.put("timestamp", System.currentTimeMillis());
        return dashboard;
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List all users (mock)",
            description = "Admin-only endpoint. Requires ADMIN role",
            security = @SecurityRequirement(name = "oauth2", scopes = {"openid", "profile", "email"})
    )
    public Map<String, Object> listUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This would return all users from the system");
        response.put("total_users", 42);
        response.put("users", List.of(
                Map.of("id", 1, "username", "user1", "email", "user1@example.com"),
                Map.of("id", 2, "username", "user2", "email", "user2@example.com")
        ));
        return response;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check - public endpoint")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Book Management API");
        health.put("keycloak_integration", "enabled");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
