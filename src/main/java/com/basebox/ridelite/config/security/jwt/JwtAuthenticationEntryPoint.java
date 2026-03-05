package com.basebox.ridelite.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point.
 * 
 * WHEN IS THIS CALLED?
 * When an unauthenticated user tries to access a protected endpoint.
 * 
 * WHAT IT DOES:
 * Returns 401 Unauthorized with error details (instead of default HTML error page).
 * 
 * EXAMPLE:
 * Request: GET /api/v1/drivers (no token)
 * Response: 401 { "error": "Unauthorized", "message": "Full authentication required" }
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        log.error("Unauthorized access attempt: {}", authException.getMessage());
        
        // ─────────────────────────────────────────────────────────────────
        // Set response status and content type
        // ─────────────────────────────────────────────────────────────────
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // ─────────────────────────────────────────────────────────────────
        // Build error response
        // ─────────────────────────────────────────────────────────────────
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getRequestURI());
        
        // ─────────────────────────────────────────────────────────────────
        // Write JSON response
        // ─────────────────────────────────────────────────────────────────
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
