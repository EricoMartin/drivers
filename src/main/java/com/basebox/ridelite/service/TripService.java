package com.basebox.ridelite.service;

import com.basebox.ridelite.domain.model.Client;
import com.basebox.ridelite.domain.model.Driver;
import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.domain.enums.TripStatus;
import com.basebox.ridelite.dto.request.CreateTripRequest;
// import com.basebox.ridelite.dto.request.UpdateTripStatusRequest;
import com.basebox.ridelite.exception.*;
import com.basebox.ridelite.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for Trip management - THE HEART OF YOUR APPLICATION!
 * 
 * STATE MACHINE MANAGEMENT:
 * REQUESTED → ASSIGNED → STARTED → COMPLETED
 *          ↓           ↓        ↓
 *       CANCELLED   CANCELLED CANCELLED
 * 
 * CRITICAL OPERATIONS:
 * - Trip creation
 * - Driver assignment (with concurrency control)
 * - State transitions (with validation)
 * - Trip completion and payment triggering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    
    private final TripRepository tripRepository;
    private final ClientService clientService;
    private final DriverService driverService;
    private final PaymentService paymentService;
    
    // =========================================================================
    // TRIP CREATION
    // =========================================================================
    
    /**
     * Create a new trip request.
     * 
     * BUSINESS RULES:
     * 1. Client cannot have multiple active trips
     * 2. Trip starts in REQUESTED status
     * 3. Driver assignment happens separately
     * 
     * TRANSACTION BEHAVIOR:
     * - Creates trip
     * - Validates client has no active trips
     * - Returns trip in REQUESTED state
     * 
     * @param request Trip creation request
     * @return Created trip
     * @throws InvalidOperationException if client has active trip
     */
    @Transactional
    public Trip createTrip(CreateTripRequest request) {
        log.info("Creating trip for client: {}", request.getClientId());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Validate client exists
        // ─────────────────────────────────────────────────────────────────
        Client client = clientService.getClientById(request.getClientId());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Check for existing active trips
        // ─────────────────────────────────────────────────────────────────
        if (tripRepository.hasActiveTripForClient(request.getClientId())) {
            log.warn("Trip creation failed: Client {} has active trip", request.getClientId());
            throw new InvalidOperationException(
                "You already have an active trip. Please complete or cancel it first."
            );
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Create trip
        // ─────────────────────────────────────────────────────────────────
        Trip trip = Trip.builder()
            .client(client)
            .status(TripStatus.REQUESTED)
            .fare(request.getFare())
            .build();
        
        trip = tripRepository.save(trip);
        
        log.info("Trip created successfully with ID: {}", trip.getId());
        return trip;
    }
    
    // =========================================================================
    // DRIVER ASSIGNMENT (CRITICAL!)
    // =========================================================================
    
    /**
     * Assign driver to a trip.
     * 
     * CRITICAL CONCURRENCY HANDLING:
     * - Uses SERIALIZABLE isolation
     * - Locks driver row (prevents double-assignment)
     * - Validates trip and driver states
     * - Updates both trip and driver atomically
     * 
     * STATE TRANSITION: REQUESTED → ASSIGNED
     * 
     * ROLLBACK SCENARIOS:
     * - No available drivers → rollback trip creation
     * - Driver becomes unavailable → rollback assignment
     * - Concurrent assignment → rollback and retry
     * 
     * @param tripId Trip ID
     * @param vehicleType Optional vehicle type requirement
     * @return Updated trip with assigned driver
     * @throws TripStatusException if trip not in REQUESTED status
     * @throws DriverNotAvailableException if no drivers available
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Trip assignDriverToTrip(UUID tripId, String vehicleType) {
        log.info("Assigning driver to trip: {} (vehicleType: {})", tripId, vehicleType);
        
        try {
            // ─────────────────────────────────────────────────────────────
            // STEP 1: Lock trip row (pessimistic lock)
            // ─────────────────────────────────────────────────────────────
            Trip trip = tripRepository.findByIdWithLock(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));
            
            // ─────────────────────────────────────────────────────────────
            // STEP 2: Validate trip is in REQUESTED status
            // ─────────────────────────────────────────────────────────────
            if (trip.getStatus() != TripStatus.REQUESTED) {
                log.warn("Cannot assign driver: Trip {} is in {} status", 
                         tripId, trip.getStatus());
                throw new TripStatusException(
                    "Cannot assign driver to trip in " + trip.getStatus() + " status"
                );
            }
            
            // ─────────────────────────────────────────────────────────────
            // STEP 3: Find and reserve available driver (locks driver row)
            // ─────────────────────────────────────────────────────────────
            Driver driver = driverService.findAndReserveDriver(vehicleType);
            
            // ─────────────────────────────────────────────────────────────
            // STEP 4: Assign driver to trip
            // ─────────────────────────────────────────────────────────────
            trip.setDriver(driver);
            trip.setStatus(TripStatus.ASSIGNED);
            trip = tripRepository.save(trip);  // @Version incremented
            
            log.info("Driver {} assigned to trip {} successfully", 
                     driver.getId(), tripId);
            return trip;
            
        } catch (OptimisticLockingFailureException e) {
            log.error("Concurrent modification during driver assignment for trip: {}", tripId);
            throw new ConcurrentModificationException(
                "Trip was modified by another process. Please try again."
            );
        }
    }
    
    // =========================================================================
    // STATE TRANSITIONS
    // =========================================================================
    
    /**
     * Update trip status with validation.
     * 
     * STATE MACHINE VALIDATION:
     * - Enforces valid state transitions
     * - Prevents invalid operations
     * - Triggers side effects (payments, driver release)
     * 
     * VALID TRANSITIONS:
     * REQUESTED → ASSIGNED (handled by assignDriverToTrip)
     * REQUESTED → CANCELLED
     * ASSIGNED → STARTED
     * ASSIGNED → CANCELLED
     * STARTED → COMPLETED
     * STARTED → CANCELLED
     * 
     * @param tripId Trip ID
     * @param newStatus New status
     * @return Updated trip
     * @throws TripStatusException if transition is invalid
     */
    @Transactional
    public Trip updateTripStatus(UUID tripId, TripStatus newStatus) {
        log.info("Updating trip {} status to: {}", tripId, newStatus);
        
        try {
            // Lock trip to prevent concurrent updates
            Trip trip = tripRepository.findByIdWithLock(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));
            
            TripStatus currentStatus = trip.getStatus();
            
            // ─────────────────────────────────────────────────────────────
            // Validate state transition
            // ─────────────────────────────────────────────────────────────
            if (!isValidTransition(currentStatus, newStatus)) {
                log.warn("Invalid status transition: {} → {}", currentStatus, newStatus);
                throw new TripStatusException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus)
                );
            }
            
            // ─────────────────────────────────────────────────────────────
            // Perform transition with side effects
            // ─────────────────────────────────────────────────────────────
            trip.setStatus(newStatus);
            
            switch (newStatus) {
                case COMPLETED -> handleTripCompletion(trip);
                case CANCELLED -> handleTripCancellation(trip);
                case STARTED -> handleTripStart(trip);
                default -> log.debug("No special handling for status: {}", newStatus);
            }
            
            trip = tripRepository.save(trip);
            
            log.info("Trip {} status updated to {} successfully", tripId, newStatus);
            return trip;
            
        } catch (OptimisticLockingFailureException e) {
            log.error("Concurrent modification during status update for trip: {}", tripId);
            throw new ConcurrentModificationException(
                "Trip was modified by another process. Please try again."
            );
        }
    }
    
    /**
     * Validate state transition.
     * 
     * @param current Current status
     * @param next Next status
     * @return true if transition is valid
     */
    private boolean isValidTransition(TripStatus current, TripStatus next) {
        return switch (current) {
            case REQUESTED -> next == TripStatus.ASSIGNED || next == TripStatus.CANCELLED;
            case ASSIGNED -> next == TripStatus.STARTED || next == TripStatus.CANCELLED;
            case STARTED -> next == TripStatus.COMPLETED || next == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;  // Terminal states
        };
    }
    
    /**
     * Handle trip start.
     */
    private void handleTripStart(Trip trip) {
        log.info("Trip {} started", trip.getId());
        // Future: Send notification to client
        // Future: Start real-time tracking
    }
    
    /**
     * Handle trip completion.
     * 
     * SIDE EFFECTS:
     * 1. Create payment
     * 2. Release driver (make available)
     * 3. Send receipt to client
     */
    private void handleTripCompletion(Trip trip) {
        log.info("Completing trip: {}", trip.getId());
        
        // Create payment
        paymentService.createPaymentForTrip(trip);
        
        // Release driver
        if (trip.getDriver() != null) {
            driverService.releaseDriver(trip.getDriver().getId());
        }
        
        // Future: Send receipt email
        // Future: Request rating
        
        log.info("Trip {} completion handling finished", trip.getId());
    }
    
    /**
     * Handle trip cancellation.
     * 
     * SIDE EFFECTS:
     * 1. Release driver if assigned
     * 2. Apply cancellation fee (if applicable)
     * 3. Send cancellation notification
     */
    private void handleTripCancellation(Trip trip) {
        log.info("Cancelling trip: {}", trip.getId());
        
        // Release driver if assigned
        if (trip.getDriver() != null) {
            driverService.releaseDriver(trip.getDriver().getId());
            log.info("Driver {} released due to trip cancellation", trip.getDriver().getId());
        }
        
        // Future: Calculate cancellation fee
        // Future: Send cancellation notification
        
        log.info("Trip {} cancellation handling finished", trip.getId());
    }
    
    // =========================================================================
    // TRIP RETRIEVAL
    // =========================================================================
    
    /**
     * Get trip by ID.
     */
    @Transactional(readOnly = true)
    public Trip getTripById(UUID tripId) {
        log.debug("Fetching trip by ID: {}", tripId);
        
        return tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));
    }
    
    /**
     * Get trip with full details (client, driver, payment).
     */
    @Transactional(readOnly = true)
    public Trip getTripWithDetails(UUID tripId) {
        log.debug("Fetching trip with full details: {}", tripId);
        
        return tripRepository.findByIdWithFullDetails(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId.toString()));
    }
    
    /**
     * Get client's trip history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Trip> getClientTrips(UUID clientId, Pageable pageable) {
        log.debug("Fetching trips for client: {}", clientId);
        
        // Verify client exists
        clientService.getClientById(clientId);
        
        return tripRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable);
    }
    
    /**
     * Get driver's trip history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Trip> getDriverTrips(UUID driverId, Pageable pageable) {
        log.debug("Fetching trips for driver: {}", driverId);
        
        // Verify driver exists
        driverService.getDriverById(driverId);
        
        return tripRepository.findByDriverIdOrderByCreatedAtDesc(driverId, pageable);
    }
    
    /**
     * Get client's active trip (if any).
     */
    @Transactional(readOnly = true)
    public Trip getClientActiveTrip(UUID clientId) {
        log.debug("Fetching active trip for client: {}", clientId);
        
        return tripRepository.findActiveTripByClientId(clientId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Active trip", "client " + clientId
            ));
    }
    
    /**
     * Get driver's active trip (if any).
     */
    @Transactional(readOnly = true)
    public Trip getDriverActiveTrip(UUID driverId) {
        log.debug("Fetching active trip for driver: {}", driverId);
        
        return tripRepository.findActiveTripByDriverId(driverId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Active trip", "driver " + driverId
            ));
    }
    
    /**
     * Get all trips by status.
     */
    @Transactional(readOnly = true)
    public Page<Trip> getTripsByStatus(TripStatus status, Pageable pageable) {
        log.debug("Fetching trips by status: {}", status);
        return tripRepository.findByStatus(status, pageable);
    }
    
    // =========================================================================
    // TRIP STATISTICS
    // =========================================================================
    
    /**
     * Count trips by status.
     */
    @Transactional(readOnly = true)
    public long countTripsByStatus(TripStatus status) {
        return tripRepository.countByStatus(status);
    }
    
    /**
     * Find stale requested trips (for cleanup job).
     * 
     * USE CASE: Background job cancels trips older than 30 minutes
     */
    @Transactional(readOnly = true)
    public List<Trip> findStaleRequestedTrips(int minutesOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesOld);
        return tripRepository.findStaleRequestedTrips(cutoff);
    }
}
