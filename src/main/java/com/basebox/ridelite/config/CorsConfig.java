package com.basebox.ridelite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * 
 * WHAT IS CORS?
 * Browser security that prevents websites from making requests to different domains.
 * 
 * Example:
 * - Your frontend: http://localhost:3000 (React app)
 * - Your backend: http://localhost:8080 (Spring Boot)
 * - Without CORS config → Browser blocks requests ❌
 * - With CORS config → Requests allowed ✅
 */
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ─────────────────────────────────────────────────────────────────
        // Allowed origins (where requests can come from)
        // ─────────────────────────────────────────────────────────────────
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",      // React development
            "http://localhost:4200",      // Angular development
            "http://localhost:8081",      // Alternative port
            "https://yourdomain.com"      // Production frontend
        ));
        
        // ─────────────────────────────────────────────────────────────────
        // Allowed HTTP methods
        // ─────────────────────────────────────────────────────────────────
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // ─────────────────────────────────────────────────────────────────
        // Allowed headers
        // ─────────────────────────────────────────────────────────────────
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept"
        ));
        
        // ─────────────────────────────────────────────────────────────────
        // Allow credentials (cookies, auth headers)
        // ─────────────────────────────────────────────────────────────────
        configuration.setAllowCredentials(true);
        
        // ─────────────────────────────────────────────────────────────────
        // How long browsers can cache CORS info (in seconds)
        // ─────────────────────────────────────────────────────────────────
        configuration.setMaxAge(3600L);
        
        // ─────────────────────────────────────────────────────────────────
        // Apply to all endpoints
        // ─────────────────────────────────────────────────────────────────
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}