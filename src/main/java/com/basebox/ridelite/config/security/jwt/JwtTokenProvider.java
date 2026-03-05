package com.basebox.ridelite.config.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT operations.
 * 
 * RESPONSIBILITIES:
 * - Generate JWT tokens
 * - Validate JWT tokens
 * - Extract claims from tokens
 * - Check token expiration
 * 
 * SECURITY NOTES:
 * - Uses HMAC SHA-512 for signing
 * - Secret key should be at least 512 bits (64 bytes)
 * - Tokens expire after configured time
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    // =========================================================================
    // CONFIGURATION (from application.properties)
    // =========================================================================
    
    /**
     * Secret key for signing tokens.
     * 
     * CRITICAL SECURITY:
     * - NEVER commit this to git!
     * - Use environment variables in production
     * - Minimum 512 bits (64 characters) for HS512
     * 
     * Generate with:
     * openssl rand -base64 64
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    /**
     * Token expiration time in milliseconds.
     * 
     * Default: 24 hours = 86400000 ms
     */
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;
    
    // =========================================================================
    // TOKEN GENERATION
    // =========================================================================
    
    /**
     * Generate JWT token from authentication.
     * 
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }
    
    /**
     * Generate JWT token from username.
     * 
     * TOKEN STRUCTURE:
     * Header: { "alg": "HS512", "typ": "JWT" }
     * Payload: { "sub": "user@example.com", "iat": 1234567890, "exp": 1234654290 }
     * Signature: HMACSHA512(header + payload, secret)
     * 
     * @param username User's email
     * @return JWT token string
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    /**
     * Generate JWT token with custom claims.
     * 
     * USE CASE: Include additional data in token (userId, role, etc.)
     * 
     * EXAMPLE:
     * Map<String, Object> claims = new HashMap<>();
     * claims.put("userId", user.getId().toString());
     * claims.put("role", user.getRole().toString());
     * String token = generateToken(claims, user.getEmail());
     * 
     * @param claims Custom data to include
     * @param username User's email
     * @return JWT token string
     */
    public String generateToken(Map<String, Object> claims, String username) {
        return createToken(claims, username);
    }
    
    /**
     * Create JWT token (internal method).
     * 
     * @param claims Token payload
     * @param subject Token subject (username)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
            .claims(claims)                          // Custom claims
            .subject(subject)                        // Username (email)
            .issuedAt(now)                          // Token creation time
            .expiration(expiryDate)                 // Token expiration time
            .signWith(getSigningKey())              // Sign with secret key
            .compact();                             // Build final token string
    }
    
    // =========================================================================
    // TOKEN VALIDATION
    // =========================================================================
    
    /**
     * Validate JWT token.
     * 
     * VALIDATION CHECKS:
     * 1. Token signature is valid (not tampered)
     * 2. Token is not expired
     * 3. Token format is correct
     * 
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // Parse and validate token
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            
            log.debug("JWT token is valid");
            return true;
            
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        
        return false;
    }
    
    // =========================================================================
    // TOKEN EXTRACTION
    // =========================================================================
    
    /**
     * Extract username (email) from token.
     * 
     * @param token JWT token
     * @return Username (email)
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract expiration date from token.
     * 
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract specific claim from token.
     * 
     * @param token JWT token
     * @param claimsResolver Function to extract claim
     * @return Extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extract all claims from token.
     * 
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    // =========================================================================
    // TOKEN EXPIRATION
    // =========================================================================
    
    /**
     * Check if token is expired.
     * 
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    // =========================================================================
    // SIGNING KEY
    // =========================================================================
    
    /**
     * Get signing key from secret.
     * 
     * SECURITY:
     * - Decodes base64 secret
     * - Creates HMAC SHA key
     * - Used for signing and verifying tokens
     * 
     * @return Secret key for signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
