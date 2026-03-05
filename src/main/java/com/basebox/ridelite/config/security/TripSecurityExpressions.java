package com.basebox.ridelite.config.security;

import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Custom security expressions.
 * 
 * USE IN @PreAuthorize:
 * @PreAuthorize("@tripSecurity.canAccessTrip(#tripId)")
 */
@Component("tripSecurity")
@RequiredArgsConstructor
public class TripSecurityExpressions {
    
    private final TripService tripService;
    
    /**
     * Check if current user can access trip.
     * 
     * RULES:
     * - Client can access their own trips
     * - Driver can access trips assigned to them
     * - Admin can access all trips
     */
    public boolean canAccessTrip(UUID tripId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        String email = authentication.getName();
        Trip trip = tripService.getTripById(tripId);
        
        // Check if user is client or driver of this trip
        return trip.getClient().getUser().getEmail().equals(email)
            || (trip.getDriver() != null && 
                trip.getDriver().getUser().getEmail().equals(email));
    }
}
