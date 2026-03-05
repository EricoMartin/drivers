package com.basebox.ridelite.controller;

import com.basebox.ridelite.config.security.jwt.JwtTokenProvider;
import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.dto.request.LoginRequest;
import com.basebox.ridelite.dto.request.RegisterUserRequest;
import com.basebox.ridelite.dto.response.EmailAvailabilityResponse;
import com.basebox.ridelite.dto.response.JwtAuthResponse;
import com.basebox.ridelite.dto.response.TokenValidationResponse;
import com.basebox.ridelite.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication and user registration.
 * 
 * ENDPOINTS:
 * - POST /api/v1/auth/register  → Register new user
 * - POST /api/v1/auth/login     → User login
 * 
 * SECURITY NOTE:
 * - These endpoints are public (no authentication required)
 * - Will add JWT tokens in security layer
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    
    // =========================================================================
    // USER REGISTRATION
    // =========================================================================
    
    /**
     * Register a new user.
     * 
     * REQUEST VALIDATION:
     * - Email format validation
     * - Password length validation
     * - Role validation
     * 
     * RESPONSE CODES:
     * - 201 Created → User registered successfully
     * - 400 Bad Request → Validation error
     * - 409 Conflict → Email already exists
     * 
     * @param request Registration details
     * @return Created user details
     */
    @PostMapping("/register")
    public ResponseEntity<JwtAuthResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {
        
        log.info("Registration request received for email: {}", request.getEmail());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Delegate to service
        // ─────────────────────────────────────────────────────────────────
        User user = userService.registerUser(request);

        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Generate JWT token
        // ─────────────────────────────────────────────────────────────────
        String token = jwtTokenProvider.generateToken(user.getEmail());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Build response
        // ─────────────────────────────────────────────────────────────────
        JwtAuthResponse response = JwtAuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Return 201 Created with response
        // ─────────────────────────────────────────────────────────────────
        log.info("User registered successfully: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return JWT token.
     * 
     * FLOW:
     * 1. Validate credentials (email + password)
     * 2. Authenticate with Spring Security
     * 3. Generate JWT token
     * 4. Return token
     * 
     * SECURITY:
     * - Password is verified using BCrypt
     * - Failed attempts are logged
     * - Token expires after configured time
     * 
     * @param request Login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("Login request received for email: {}", request.getEmail());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Authenticate user
        // ─────────────────────────────────────────────────────────────────
        // This calls CustomUserDetailsService.loadUserByUsername()
        // Then verifies password using BCrypt
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Set authentication in SecurityContext
        // ─────────────────────────────────────────────────────────────────
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Generate JWT token
        // ─────────────────────────────────────────────────────────────────
        String token = jwtTokenProvider.generateToken(authentication);
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 4: Get user details
        // ─────────────────────────────────────────────────────────────────
        User user = userService.getUserByEmail(request.getEmail());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 5: Build response
        // ─────────────────────────────────────────────────────────────────
        JwtAuthResponse response = JwtAuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
        
        log.info("User logged in successfully: {}", user.getId());
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // TOKEN VALIDATION
    // =========================================================================
    
    /**
     * Validate JWT token.
     * 
     * USE CASE: Frontend can check if token is still valid
     * 
     * @param token JWT token (from Authorization header)
     * @return Validation result
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String token) {
        
        // Extract token (remove "Bearer " prefix)
        String jwt = token.substring(7);
        
        boolean isValid = jwtTokenProvider.validateToken(jwt);
        
        TokenValidationResponse response = TokenValidationResponse.builder()
            .valid(isValid)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // USER LOGIN
    // =========================================================================
    
    // /**
    //  * Authenticate user.
    //  * 
    //  * NOTE: Simplified for now - will add JWT in security layer
    //  * 
    //  * RESPONSE CODES:
    //  * - 200 OK → Login successful
    //  * - 401 Unauthorized → Invalid credentials
    //  * 
    //  * @param request Login credentials
    //  * @return Authentication token (future: JWT)
    //  */
    // @PostMapping("/login")
    // public ResponseEntity<AuthResponse> login(
    //         @Valid @RequestBody LoginRequest request) {
        
    //     log.info("Login request received for email: {}", request.getEmail());
        
    //     // Future: Implement JWT authentication
    //     // For now, just verify user exists
    //     User user = userService.getUserByEmail(request.getEmail());
        
    //     AuthResponse response = AuthResponse.builder()
    //         .userId(user.getId())
    //         .email(user.getEmail())
    //         .role(user.getRole())
    //         .message("Login successful (JWT will be added later)")
    //         .build();
        
    //     log.info("User logged in successfully: {}", user.getId());
    //     return ResponseEntity.ok(response);
    // }
    
    // =========================================================================
    // EMAIL AVAILABILITY CHECK
    // =========================================================================
    
    /**
     * Check if email is available.
     * 
     * USE CASE: Frontend validation before form submission
     * 
     * @param email Email to check
     * @return Availability status
     */
    @GetMapping("/check-email")
    public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(
            @RequestParam String email) {
        
        log.debug("Checking email availability: {}", email);
        
        boolean available = !userService.isEmailRegistered(email);
        
        EmailAvailabilityResponse response = EmailAvailabilityResponse.builder()
            .email(email)
            .available(available)
            .build();
        
        return ResponseEntity.ok(response);
    }
}