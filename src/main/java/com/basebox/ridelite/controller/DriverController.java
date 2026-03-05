package com.basebox.ridelite.controller;

import com.basebox.ridelite.config.security.SecurityUtils;
import com.basebox.ridelite.domain.model.Driver;
import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.dto.request.UpdateDriverAvailabilityRequest;
import com.basebox.ridelite.dto.request.UpdateVehicleTypeRequest;
import com.basebox.ridelite.dto.response.DriverResponse;
import com.basebox.ridelite.dto.response.DriverStatsResponse;
import com.basebox.ridelite.exception.InvalidOperationException;
import com.basebox.ridelite.service.DriverService;
import com.basebox.ridelite.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for driver operations.
 * 
 * ENDPOINTS:
 * - GET    /api/v1/drivers/{id}                    → Get driver by ID
 * - GET    /api/v1/drivers/user/{userId}           → Get driver by user ID
 * - GET    /api/v1/drivers/available               → Get available drivers
 * - PUT    /api/v1/drivers/{id}/availability       → Update availability
 * - PUT    /api/v1/drivers/{id}/vehicle-type       → Update vehicle type
 * - GET    /api/v1/drivers/{id}/stats              → Get driver statistics
 */
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DriverController {
    
    private final DriverService driverService;
    private final UserService userService;
    
    // =========================================================================
    // DRIVER RETRIEVAL
    // =========================================================================
    
    /**
     * Get driver by ID.
     * 
     * @param id Driver ID
     * @return Driver details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable UUID id) {
        log.debug("GET request for driver ID: {}", id);
        
        Driver driver = driverService.getDriverById(id);
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get driver by user ID.
     * 
     * USE CASE: User logs in, frontend needs their driver profile
     * 
     * @param userId User ID
     * @return Driver details
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<DriverResponse> getDriverByUserId(@PathVariable UUID userId) {
        log.debug("GET request for driver by user ID: {}", userId);
        
        Driver driver = driverService.getDriverByUserId(userId);
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all available drivers.
     * 
     * QUERY PARAMS:
     * - vehicleType (optional): Filter by vehicle type
     * 
     * ACCESS: CLIENT role only (clients looking for drivers)
     * 
     * @param vehicleType Optional vehicle type filter
     * @return List of available drivers
     */
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @RequestParam(required = false) String vehicleType) {
        
        log.debug("GET request for available drivers (vehicleType: {})", vehicleType);
        
        List<Driver> drivers;
        if (vehicleType != null && !vehicleType.isBlank()) {
            drivers = driverService.getAvailableDriversByVehicleType(vehicleType);
        } else {
            drivers = driverService.getAvailableDrivers();
        }
        
        List<DriverResponse> response = drivers.stream()
            .map(DriverResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get count of available drivers.
     * 
     * @return Count of available drivers
     */
    @GetMapping("/available/count")
    public ResponseEntity<Long> countAvailableDrivers() {
        log.debug("GET request for available driver count");
        
        long count = driverService.countAvailableDrivers();
        return ResponseEntity.ok(count);
    }
    
    // =========================================================================
    // DRIVER AVAILABILITY MANAGEMENT
    // =========================================================================
    
    /**
     * Update driver availability.
     * 
     * BUSINESS RULES:
     * - Driver cannot go offline with active trips
     * - Uses optimistic locking (retry on conflict)
     * 
     * ACCESS: Only DRIVER role
     * RULE: Driver can only update their own availability
     * 
     * @param id Driver ID
     * @param request Availability update
     * @return Updated driver
     */
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDriverAvailabilityRequest request) {
        
        log.info("PUT request to update driver {} availability to: {}", 
                 id, request.getAvailable());

        String email = SecurityUtils.getCurrentUserEmail();
        User currentUser = userService.getUserByEmail(email);

        Driver driver = driverService.getDriverById(id);

        if (!driver.getUser().getId().equals(currentUser.getId())) {
            throw new InvalidOperationException("You can only update your own profile");
        }
        
        driver = driverService.setDriverAvailability(id, request.getAvailable());
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark driver as available (convenience endpoint).
     * 
     * @param id Driver ID
     * @return Updated driver
     */
    @PostMapping("/{id}/go-online")
    public ResponseEntity<DriverResponse> goOnline(@PathVariable UUID id) {
        log.info("POST request to mark driver {} online", id);
        
        Driver driver = driverService.markDriverAvailable(id);
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark driver as unavailable (convenience endpoint).
     * 
     * @param id Driver ID
     * @return Updated driver
     */
    @PostMapping("/{id}/go-offline")
    public ResponseEntity<DriverResponse> goOffline(@PathVariable UUID id) {
        log.info("POST request to mark driver {} offline", id);
        
        Driver driver = driverService.markDriverUnavailable(id);
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // DRIVER PROFILE MANAGEMENT
    // =========================================================================
    
    /**
     * Update driver vehicle type.
     * 
     * @param id Driver ID
     * @param request Vehicle type update
     * @return Updated driver
     */
    @PutMapping("/{id}/vehicle-type")
    public ResponseEntity<DriverResponse> updateVehicleType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleTypeRequest request) {
        
        log.info("PUT request to update driver {} vehicle type to: {}", 
                 id, request.getVehicleType());
        
        Driver driver = driverService.updateVehicleType(id, request.getVehicleType());
        DriverResponse response = DriverResponse.from(driver);
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // DRIVER STATISTICS
    // =========================================================================
    
    /**
     * Get driver statistics.
     * 
     * INCLUDES:
     * - Total trips completed
     * - Total earnings
     * - Average fare
     * - Current availability
     * 
     * @param id Driver ID
     * @return Driver statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<DriverStatsResponse> getDriverStats(@PathVariable UUID id) {
        log.debug("GET request for driver {} statistics", id);
        
        DriverStatsResponse stats = driverService.getDriverStats(id);
        return ResponseEntity.ok(stats);
    }
}
