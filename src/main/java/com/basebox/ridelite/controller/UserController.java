package com.basebox.ridelite.controller;

import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.domain.enums.Role;
import com.basebox.ridelite.dto.request.ChangePasswordRequest;
import com.basebox.ridelite.dto.response.UserResponse;
import com.basebox.ridelite.dto.response.MessageResponse;
import com.basebox.ridelite.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for user management operations.
 * 
 * ENDPOINTS:
 * - GET    /api/v1/users/{id}           → Get user by ID
 * - GET    /api/v1/users/email/{email}  → Get user by email
 * - GET    /api/v1/users/role/{role}    → Get users by role
 * - PUT    /api/v1/users/{id}/password  → Change password
 * 
 * SECURITY NOTE:
 * - Future: Add @PreAuthorize for role-based access
 * - Users should only access their own data (except admins)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {
    
    private final UserService userService;
    
    // =========================================================================
    // USER RETRIEVAL
    // =========================================================================
    
    /**
     * Get user by ID.
     * 
     * @param id User ID
     * @return User details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        log.debug("GET request for user ID: {}", id);
        
        User user = userService.getUserById(id);
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user by email.
     * 
     * @param email User email
     * @return User details
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        log.debug("GET request for user email: {}", email);
        
        User user = userService.getUserByEmail(email);
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all users by role.
     * 
     * NOTE: Should be admin-only in production
     * 
     * @param role User role
     * @return List of users
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable Role role) {
        log.debug("GET request for users with role: {}", role);
        
        List<User> users = userService.getUsersByRole(role);
        List<UserResponse> response = users.stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user statistics by role.
     * 
     * @param role User role
     * @return User count
     */
    @GetMapping("/role/{role}/count")
    public ResponseEntity<Long> countUsersByRole(@PathVariable Role role) {
        log.debug("GET request for user count by role: {}", role);
        
        long count = userService.countUsersByRole(role);
        return ResponseEntity.ok(count);
    }
    
    // =========================================================================
    // PASSWORD MANAGEMENT
    // =========================================================================
    
    /**
     * Change user password.
     * 
     * SECURITY:
     * - Requires old password verification
     * - Future: Add rate limiting (prevent brute force)
     * 
     * @param id User ID
     * @param request Password change request
     * @return Success message
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<MessageResponse> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        log.info("Password change request for user: {}", id);
        
        userService.changePassword(id, request.getOldPassword(), request.getNewPassword());
        
        MessageResponse response = MessageResponse.builder()
            .message("Password changed successfully")
            .build();
        
        return ResponseEntity.ok(response);
    }
}
