package com.basebox.ridelite.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility to get current authenticated user.
 */
@Component
public class SecurityUtils {
    
    /**
     * Get current user's email.
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        
        return null;
    }
    
    /**
     * Check if user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        return authentication != null && authentication.isAuthenticated();
    }
    
    /**
     * Check if current user has specific role.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        
        return false;
    }
}
