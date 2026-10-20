package com.example.untitled.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.Components;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        // Define security schemes
        SecurityScheme bearerAuthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from Keycloak");

        SecurityScheme oauth2Scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl(issuerUri + "/protocol/openid-connect/auth")
                                .tokenUrl(issuerUri + "/protocol/openid-connect/token")
                                .scopes(new Scopes()
                                        .addString("openid", "OpenID Connect scope")
                                        .addString("profile", "User profile scope")
                                        .addString("email", "Email scope"))));

        return new OpenAPI()
                .info(new Info()
                        .title("Books API with Keycloak Security")
                        .version("1.0.0")
                        .description("REST API for managing a library of books with comprehensive CRUD operations, " +
                                "bulk CSV imports (traditional and virtual threads), high-performance data generation, " +
                                "and **Keycloak OAuth2/OpenID Connect authentication**. " +
                                "Optimized for handling large datasets efficiently with role-based access control.")
                        .contact(new Contact()
                                .name("Books API Support")
                                .url("https://example.com")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                // No explicit servers: springdoc derives the server URL from the incoming request,
                // so Swagger UI works on any host/port (local 9090, Docker 8080, ...).
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", bearerAuthScheme)
                        .addSecuritySchemes("oauth2", oauth2Scheme))
                .addSecurityItem(new SecurityRequirement().addList("oauth2",
                        Arrays.asList("openid", "profile", "email")));
    }
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(200 * 1024 * 1024); // 200MB
        });
        return factory;
    }
    @Bean
    public MultipartConfigElement multipartConfig() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(200));
        factory.setMaxRequestSize(DataSize.ofMegabytes(200));
        return factory.createMultipartConfig();
    }
}
