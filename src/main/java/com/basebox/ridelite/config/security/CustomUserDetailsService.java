package com.basebox.ridelite.config.security;

import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetailsService implementation.
 * 
 * WHAT IS UserDetailsService?
 * Spring Security interface that loads user data for authentication.
 * 
 * WHEN IS IT CALLED?
 * - During login (authentication)
 * - When validating JWT tokens
 * - When checking permissions
 * 
 * RESPONSIBILITIES:
 * - Load user from database by email
 * - Convert User entity to Spring Security UserDetails
 * - Provide user's authorities (roles/permissions)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Load user by username (email in our case).
     * 
     * FLOW:
     * 1. Spring Security calls this with email
     * 2. We fetch user from database
     * 3. Convert to UserDetails object
     * 4. Spring Security uses it for authentication
     * 
     * @param email User's email
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Fetch user from database
        // ─────────────────────────────────────────────────────────────────
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("User not found with email: {}", email);
                return new UsernameNotFoundException(
                    "User not found with email: " + email
                );
            });
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Convert to UserDetails
        // ─────────────────────────────────────────────────────────────────
        return buildUserDetails(user);
    }
    
    /**
     * Convert User entity to Spring Security UserDetails.
     * 
     * UserDetails INTERFACE REQUIRES:
     * - username (we use email)
     * - password (hashed)
     * - authorities (roles/permissions)
     * - account status flags (enabled, locked, expired, etc.)
     * 
     * @param user User entity
     * @return UserDetails object
     */
    private UserDetails buildUserDetails(User user) {
        // ─────────────────────────────────────────────────────────────────
        // Convert role to Spring Security authority
        // ─────────────────────────────────────────────────────────────────
        Collection<GrantedAuthority> authorities = getAuthorities(user);
        
        // ─────────────────────────────────────────────────────────────────
        // Build Spring Security User object
        // ─────────────────────────────────────────────────────────────────
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())              // Email as username
            .password(user.getPassword())           // Hashed password
            .authorities(authorities)               // User's roles
            .accountExpired(false)                  // Account not expired
            .accountLocked(false)                   // Account not locked
            .credentialsExpired(false)              // Password not expired
            .disabled(false)                        // Account is active
            .build();
    }
    
    /**
     * Get user authorities (roles).
     * 
     * ROLE NAMING CONVENTION:
     * Spring Security expects roles to start with "ROLE_"
     * - User has role: DRIVER
     * - Authority: ROLE_DRIVER
     * - Check with: hasRole('DRIVER') or hasAuthority('ROLE_DRIVER')
     * 
     * @param user User entity
     * @return Collection of authorities
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        // Convert role to authority with "ROLE_" prefix
        String roleName = "ROLE_" + user.getRole().name();
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        
        log.debug("User {} has authority: {}", user.getEmail(), roleName);
        
        return Collections.singletonList(authority);
    }
}
