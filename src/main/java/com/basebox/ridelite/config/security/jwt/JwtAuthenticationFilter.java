package com.basebox.ridelite.config.security.jwt;

import com.basebox.ridelite.config.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 * 
 * WHAT IS A FILTER?
 * Intercepts every HTTP request BEFORE it reaches the controller.
 * Think of it as airport security - checks everyone before they board.
 * 
 * FILTER FLOW:
 * HTTP Request → JwtAuthenticationFilter → Spring Security → Controller
 * 
 * RESPONSIBILITIES:
 * 1. Extract JWT token from Authorization header
 * 2. Validate token
 * 3. Load user details
 * 4. Set authentication in Spring Security context
 * 5. Pass request to next filter
 * 
 * EXTENDS OncePerRequestFilter:
 * Ensures this filter runs exactly once per request (not multiple times)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    /**
     * Filter every HTTP request.
     * 
     * EXECUTION ORDER:
     * 1. Extract JWT from header
     * 2. Validate JWT
     * 3. Extract username from JWT
     * 4. Load user from database
     * 5. Create authentication object
     * 6. Set in SecurityContext
     * 7. Continue filter chain
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Next filters in chain
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // ─────────────────────────────────────────────────────────────
            // STEP 1: Extract JWT token from Authorization header
            // ─────────────────────────────────────────────────────────────
            String jwt = extractJwtFromRequest(request);
            
            // ─────────────────────────────────────────────────────────────
            // STEP 2: Validate token and authenticate user
            // ─────────────────────────────────────────────────────────────
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                
                // Extract username from token
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                log.debug("JWT token validated for user: {}", username);
                
                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,                // Principal (the user)
                        null,                       // Credentials (not needed - already authenticated)
                        userDetails.getAuthorities() // Authorities (roles)
                    );
                
                // Set request details
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // ─────────────────────────────────────────────────────────
                // STEP 3: Set authentication in SecurityContext
                // ─────────────────────────────────────────────────────────
                // This tells Spring Security: "This request is authenticated"
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Set authentication for user: {}", username);
            }
            
        } catch (Exception ex) {
            // Log but don't throw - let request continue unauthenticated
            log.error("Could not set user authentication in security context", ex);
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 4: Continue filter chain (pass to next filter)
        // ─────────────────────────────────────────────────────────────────
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * HEADER FORMAT:
     * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWI...
     * 
     * EXTRACTION:
     * 1. Get "Authorization" header
     * 2. Check if it starts with "Bearer "
     * 3. Remove "Bearer " prefix
     * 4. Return the token
     * 
     * @param request HTTP request
     * @return JWT token or null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Check if header exists and has Bearer format
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix (7 characters)
            String token = bearerToken.substring(7);
            log.debug("Extracted JWT token from Authorization header");
            return token;
        }
        
        return null;
    }
}
