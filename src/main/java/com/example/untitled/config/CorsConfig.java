package com.example.untitled.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for the Books API.
 * Allows cross-origin requests from frontend applications.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",      // React dev server
                        "http://localhost:8080",      // Docker API
                        "http://localhost:9090",      // Local dev API
                        "http://127.0.0.1:3000",      // Localhost variant
                        "http://127.0.0.1:8080",      // Localhost variant
                        "http://127.0.0.1:9090",      // Localhost variant
                        "https://localhost:9090",     // HTTPS variant
                        "https://127.0.0.1:9090"      // HTTPS localhost
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);  // Cache preflight response for 1 hour
    }
}

