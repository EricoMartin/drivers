package com.basebox.ridelite.service;

import com.basebox.ridelite.domain.model.Driver;
import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.dto.response.DriverStatsResponse;
import com.basebox.ridelite.exception.ConcurrentModificationException;
import com.basebox.ridelite.exception.DriverNotAvailableException;
import com.basebox.ridelite.exception.InvalidOperationException;
import com.basebox.ridelite.exception.ResourceNotFoundException;
import com.basebox.ridelite.repository.DriverRepository;
import com.basebox.ridelite.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Service for Driver management and availability control.
 * 
 * CRITICAL BUSINESS LOGIC:
 * - Driver availability management (with concurrency control)
 * - Trip assignment validation
 * - Driver statistics and earnings
 * 
 * CONCURRENCY STRATEGY:
 * - Uses pessimistic locking for availability updates
 * - Handles OptimisticLockingFailureException gracefully
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    
    // =========================================================================
    // DRIVER RETRIEVAL
    // =========================================================================
    
    /**
     * Get driver by ID.
     * 
     * @param driverId Driver ID
     * @return Driver entity
     * @throws ResourceNotFoundException if driver not found
     */
    @Transactional(readOnly = true)
    public Driver getDriverById(UUID driverId) {
        log.debug("Fetching driver by ID: {}", driverId);
        
        return driverRepository.findById(driverId)
            .orElseThrow(() -> {
                log.error("Driver not found with ID: {}", driverId);
                return new ResourceNotFoundException("Driver", driverId.toString());
            });
    }
    
    /**
     * Get driver by user ID.
     * 
     * USE CASE: User logs in, retrieve their driver profile
     * 
     * @param userId User ID
     * @return Driver entity
     * @throws ResourceNotFoundException if driver profile not found
     */
    @Transactional(readOnly = true)
    public Driver getDriverByUserId(UUID userId) {
        log.debug("Fetching driver by user ID: {}", userId);
        
        return driverRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.error("Driver profile not found for user ID: {}", userId);
                return new ResourceNotFoundException("Driver profile", userId.toString());
            });
    }
    
    /**
     * Get all available drivers.
     * 
     * @return List of available drivers
     */
    @Transactional(readOnly = true)
    public List<Driver> getAvailableDrivers() {
        log.debug("Fetching all available drivers");
        return driverRepository.findByIsAvailableTrue();
    }
    
    /**
     * Get available drivers by vehicle type.
     * 
     * USE CASE: Client requests specific vehicle type
     * 
     * @param vehicleType Vehicle type (e.g., "Sedan", "SUV")
     * @return List of available drivers
     */
    @Transactional(readOnly = true)
    public List<Driver> getAvailableDriversByVehicleType(String vehicleType) {
        log.debug("Fetching available drivers by vehicle type: {}", vehicleType);
        return driverRepository.findByIsAvailableTrueAndVehicleType(vehicleType);
    }
    
    // =========================================================================
    // AVAILABILITY MANAGEMENT (CRITICAL SECTION!)
    // =========================================================================
    
    /**
     * Set driver availability.
     * 
     * BUSINESS RULES:
     * 1. Driver cannot go unavailable if they have active trips
     * 2. Uses optimistic locking to handle concurrent updates
     * 
     * CONCURRENCY HANDLING:
     * - @Version field prevents lost updates
     * - If update fails, retry logic can be added in controller/scheduler
     * 
     * @param driverId Driver ID
     * @param available New availability status
     * @throws InvalidOperationException if driver has active trips
     * @throws ConcurrentModificationException if concurrent update detected
     */
    @Transactional
    public Driver setDriverAvailability(UUID driverId, boolean available) {
        log.info("Setting driver {} availability to: {}", driverId, available);
        
        try {
            // Fetch driver (optimistic locking via @Version)
            Driver driver = getDriverById(driverId);
            
            // ─────────────────────────────────────────────────────────────
            // BUSINESS RULE: Can't go offline with active trips
            // ─────────────────────────────────────────────────────────────
            if (!available && tripRepository.hasActiveTripForDriver(driverId)) {
                log.warn("Cannot set driver {} unavailable: has active trips", driverId);
                throw new InvalidOperationException(
                    "Cannot go offline while you have active trips"
                );
            }
            
            // Update availability
            driver.setIsAvailable(available);
            driver = driverRepository.save(driver);  // @Version incremented
            
            log.info("Driver {} availability updated successfully", driverId);
            return driver;
            
        } catch (OptimisticLockingFailureException e) {
            // Someone else modified the driver record
            log.error("Concurrent modification detected for driver: {}", driverId);
            throw new ConcurrentModificationException(
                "Driver record was modified by another process. Please try again."
            );
        }
    }
    
    /**
     * Mark driver as available.
     * 
     * @param driverId Driver ID
     * @return Updated driver
     */
    @Transactional
    public Driver markDriverAvailable(UUID driverId) {
        log.info("Marking driver {} as available", driverId);
        return setDriverAvailability(driverId, true);
    }
    
    /**
     * Mark driver as unavailable.
     * 
     * @param driverId Driver ID
     * @return Updated driver
     */
    @Transactional
    public Driver markDriverUnavailable(UUID driverId) {
        log.info("Marking driver {} as unavailable", driverId);
        return setDriverAvailability(driverId, false);
    }
    
    // =========================================================================
    // DRIVER ASSIGNMENT (Used by TripService)
    // =========================================================================
    
    /**
     * Find and reserve an available driver for a trip.
     * 
     * CRITICAL CONCURRENCY CONTROL:
     * - Uses PESSIMISTIC WRITE lock (FOR UPDATE in SQL)
     * - Blocks other transactions from reading this driver
     * - Prevents double-assignment of same driver
     * 
     * TRANSACTION ISOLATION:
     * - SERIALIZABLE: Highest isolation level
     * - Prevents phantom reads and dirty reads
     * - Essential for money-critical operations
     * 
     * ALGORITHM:
     * 1. Lock first available driver (blocks others)
     * 2. Verify still available
     * 3. Mark as unavailable
     * 4. Return driver
     * 5. Lock released on transaction commit
     * 
     * @param vehicleType Optional vehicle type filter
     * @return Available driver (now locked)
     * @throws DriverNotAvailableException if no drivers available
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Driver findAndReserveDriver(String vehicleType) {
        log.info("Finding and reserving driver (vehicleType: {})", vehicleType);
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Find available driver with PESSIMISTIC LOCK
        // ─────────────────────────────────────────────────────────────────
        Driver driver;
        if (vehicleType != null && !vehicleType.isBlank()) {
            driver = driverRepository
                .findFirstAvailableDriverByVehicleTypeWithLock(vehicleType)
                .orElseThrow(() -> {
                    log.warn("No available drivers found for vehicle type: {}", vehicleType);
                    return new DriverNotAvailableException(
                        "No available drivers for vehicle type: " + vehicleType
                    );
                });
        } else {
            driver = driverRepository
                .findFirstAvailableDriverWithLock()
                .orElseThrow(() -> {
                    log.warn("No available drivers found");
                    return new DriverNotAvailableException("No available drivers at this time");
                });
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Double-check availability (defensive programming)
        // ─────────────────────────────────────────────────────────────────
        if (!driver.getIsAvailable()) {
            log.error("Driver {} was not actually available despite query", driver.getId());
            throw new DriverNotAvailableException("Driver is no longer available");
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Reserve driver (mark unavailable)
        // ─────────────────────────────────────────────────────────────────
        driver.setIsAvailable(false);
        driver = driverRepository.save(driver);
        
        log.info("Driver {} reserved successfully", driver.getId());
        return driver;
    }
    
    /**
     * Release driver (make available again).
     * 
     * USE CASE: Trip completed or cancelled
     * 
     * @param driverId Driver ID
     */
    @Transactional
    public void releaseDriver(UUID driverId) {
        log.info("Releasing driver: {}", driverId);
        
        Driver driver = getDriverById(driverId);
        driver.setIsAvailable(true);
        driverRepository.save(driver);
        
        log.info("Driver {} released successfully", driverId);
    }
    
    // =========================================================================
    // DRIVER STATISTICS
    // =========================================================================
    
    /**
     * Get driver statistics (earnings, trip count, etc.).
     * 
     * @param driverId Driver ID
     * @return Driver statistics
     */
    @Transactional(readOnly = true)
    public DriverStatsResponse getDriverStats(UUID driverId) {
        log.debug("Fetching statistics for driver: {}", driverId);
        
        // Verify driver exists
        Driver driver = getDriverById(driverId);
        
        // Fetch statistics
        List<Trip> completedTrips = tripRepository.findCompletedTripsByDriver(driverId);
        BigDecimal totalEarnings = tripRepository.calculateDriverEarnings(driverId);
        long tripCount = completedTrips.size();
        
        // Calculate average fare
        BigDecimal avgFare = tripCount > 0 
            ? totalEarnings.divide(BigDecimal.valueOf(tripCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return DriverStatsResponse.builder()
            .driverId(driverId)
            .totalTrips(tripCount)
            .totalEarnings(totalEarnings)
            .averageFare(avgFare)
            .isAvailable(driver.getIsAvailable())
            .vehicleType(driver.getVehicleType())
            .build();
    }
    
    /**
     * Update driver vehicle type.
     * 
     * @param driverId Driver ID
     * @param vehicleType New vehicle type
     * @return Updated driver
     */
    @Transactional
    public Driver updateVehicleType(UUID driverId, String vehicleType) {
        log.info("Updating vehicle type for driver {}: {}", driverId, vehicleType);
        
        Driver driver = getDriverById(driverId);
        driver.setVehicleType(vehicleType);
        driver = driverRepository.save(driver);
        
        log.info("Vehicle type updated successfully for driver: {}", driverId);
        return driver;
    }
    
    /**
     * Get count of available drivers.
     * 
     * @return Count of available drivers
     */
    @Transactional(readOnly = true)
    public long countAvailableDrivers() {
        return driverRepository.countByIsAvailableTrue();
    }
}
