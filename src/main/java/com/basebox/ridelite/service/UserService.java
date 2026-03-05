package com.basebox.ridelite.service;

import com.basebox.ridelite.domain.model.Client;
import com.basebox.ridelite.domain.model.Driver;
import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.domain.enums.Role;
import com.basebox.ridelite.dto.request.RegisterUserRequest;
// import com.basebox.ridelite.dto.response.UserResponse;
import com.basebox.ridelite.exception.InvalidOperationException;
import com.basebox.ridelite.exception.ResourceNotFoundException;
import com.basebox.ridelite.repository.ClientRepository;
import com.basebox.ridelite.repository.DriverRepository;
import com.basebox.ridelite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for User management and authentication.
 * 
 * RESPONSIBILITIES:
 * - User registration (with role-specific profile creation)
 * - User authentication
 * - User profile management
 * - Password management
 * 
 * TRANSACTION STRATEGY:
 * - Write operations: @Transactional (default)
 * - Read operations: @Transactional(readOnly = true)
 */
@Service
@RequiredArgsConstructor
@Slf4j  // Creates: private static final Logger log = LoggerFactory.getLogger(UserService.class);
public class UserService {
    
    // =========================================================================
    // DEPENDENCIES (Injected via constructor)
    // =========================================================================
    
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;  // For password hashing
    
    // =========================================================================
    // USER REGISTRATION
    // =========================================================================
    
    /**
     * Register a new user with role-specific profile.
     * 
     * BUSINESS RULES:
     * 1. Email must be unique
     * 2. Password must be hashed (never store plain text!)
     * 3. DRIVER role → create Driver profile
     * 4. CLIENT role → create Client profile
     * 
     * TRANSACTION BEHAVIOR:
     * - Creates User + Driver/Client in single transaction
     * - If profile creation fails, user creation is rolled back
     * 
     * @param request Registration details
     * @return Created user with profile
     * @throws InvalidOperationException if email already exists
     */
    @Transactional
    public User registerUser(RegisterUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Validate email uniqueness
        // ─────────────────────────────────────────────────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new InvalidOperationException(
                "Email already registered: " + request.getEmail()
            );
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Create User entity
        // ─────────────────────────────────────────────────────────────────
        User user = User.builder()
            .username(request.getUsername())
            .firstName(request.getFirstName())
            .middleName(request.getMiddleName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))  // Hash password!
            .role(request.getRole())
            .build();
        
        // Save user first (generates ID)
        user = userRepository.save(user);
        log.debug("User created with ID: {}", user.getId());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Create role-specific profile
        // ─────────────────────────────────────────────────────────────────
        switch (request.getRole()) {
            case DRIVER -> {
                Driver driver = Driver.builder()
                    .user(user)
                    .isAvailable(false)  // New drivers start unavailable
                    .vehicleType(request.getVehicleType())
                    .build();
                driverRepository.save(driver);
                user.setDriver(driver);  // Bidirectional relationship
                log.info("Driver profile created for user: {}", user.getId());
            }
            case CLIENT -> {
                Client client = Client.builder()
                    .user(user)
                    .build();
                clientRepository.save(client);
                user.setClient(client);  // Bidirectional relationship
                log.info("Client profile created for user: {}", user.getId());
            }
        }
        
        log.info("User registration completed successfully: {}", user.getEmail());
        return user;
    }
    
    // =========================================================================
    // USER RETRIEVAL
    // =========================================================================
    
    /**
     * Find user by ID.
     * 
     * @param userId User ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("User not found with ID: {}", userId);
                return new ResourceNotFoundException("User", userId.toString());
            });
    }
    
    /**
     * Find user by email.
     * 
     * @param email User email
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        
        return userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("User not found with email: {}", email);
                return new ResourceNotFoundException("User", email);
            });
    }
    
    /**
     * Find user by email with role-specific profile loaded.
     * 
     * USE CASE: Login - need user + driver/client profile
     * 
     * OPTIMIZATION: Uses JOIN FETCH to avoid N+1 queries
     * 
     * @param email User email
     * @return User with profile loaded
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmailWithProfile(String email) {
        log.debug("Fetching user with profile by email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", email));
        
        // Load profile based on role
        return switch (user.getRole()) {
            case DRIVER -> userRepository.findByEmailWithDriver(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
            case CLIENT -> userRepository.findByEmailWithClient(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        };
    }
    
    /**
     * Get all users by role.
     * 
     * @param role User role
     * @return List of users
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        log.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role);
    }
    
    // =========================================================================
    // PASSWORD MANAGEMENT
    // =========================================================================
    
    /**
     * Change user password.
     * 
     * SECURITY:
     * - Validates old password before changing
     * - Hashes new password
     * 
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password
     * @throws InvalidOperationException if old password is incorrect
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);
        
        User user = getUserById(userId);
        
        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Password change failed: Incorrect old password for user: {}", userId);
            throw new InvalidOperationException("Incorrect current password");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }
    
    /**
     * Reset password (admin function or forgot password flow).
     * 
     * @param userId User ID
     * @param newPassword New password
     */
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);
        
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password reset successfully for user: {}", userId);
    }
    
    // =========================================================================
    // USER STATISTICS
    // =========================================================================
    
    /**
     * Get count of users by role.
     * 
     * @param role User role
     * @return Count of users
     */
    @Transactional(readOnly = true)
    public long countUsersByRole(Role role) {
        log.debug("Counting users by role: {}", role);
        return userRepository.countByRole(role);
    }
    
    /**
     * Check if email is already registered.
     * 
     * @param email Email to check
     * @return true if email exists
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }
}