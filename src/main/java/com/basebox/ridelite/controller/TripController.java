package com.basebox.ridelite.controller;

import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.domain.enums.TripStatus;
import com.basebox.ridelite.dto.request.AssignDriverRequest;
import com.basebox.ridelite.dto.request.CreateTripRequest;
import com.basebox.ridelite.dto.request.UpdateTripStatusRequest;
import com.basebox.ridelite.dto.response.TripResponse;
import com.basebox.ridelite.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * Controller for trip management - THE CORE OF YOUR API!
 * 
 * ENDPOINTS:
 * - POST   /api/v1/trips                        → Create trip
 * - GET    /api/v1/trips/{id}                   → Get trip by ID
 * - PUT    /api/v1/trips/{id}/assign-driver     → Assign driver
 * - PUT    /api/v1/trips/{id}/status            → Update status
 * - GET    /api/v1/trips/client/{clientId}      → Get client's trips
 * - GET    /api/v1/trips/driver/{driverId}      → Get driver's trips
 * - GET    /api/v1/trips/client/{clientId}/active → Get active trip
 * - GET    /api/v1/trips/status/{status}        → Get trips by status
 */
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TripController {
    
    private final TripService tripService;
    
    // =========================================================================
    // TRIP CREATION
    // =========================================================================
    
    /**
     * Create a new trip request.
     * 
     * BUSINESS FLOW:
     * 1. Client requests trip
     * 2. Trip created in REQUESTED status
     * 3. Driver assignment happens separately
     * 
     * ACCESS: CLIENT role only
     * 
     * RESPONSE CODES:
     * - 201 Created → Trip created successfully
     * - 400 Bad Request → Validation error
     * - 409 Conflict → Client has active trip
     * 
     * @param request Trip creation request
     * @return Created trip
     */
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request) {
        
        log.info("POST request to create trip for client: {}", request.getClientId());
        
        Trip trip = tripService.createTrip(request);
        TripResponse response = TripResponse.from(trip);
        
        log.info("Trip created successfully with ID: {}", trip.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // =========================================================================
    // DRIVER ASSIGNMENT
    // =========================================================================
    
    /**
     * Assign driver to trip.
     * 
     * CRITICAL OPERATION:
     * - Uses pessimistic locking
     * - Prevents concurrent driver assignment
     * - Updates trip status to ASSIGNED
     * 
     * RESPONSE CODES:
     * - 200 OK → Driver assigned
     * - 404 Not Found → Trip not found
     * - 409 Conflict → Trip not in REQUESTED status or no drivers available
     * 
     * @param id Trip ID
     * @param request Driver assignment request
     * @return Updated trip with assigned driver
     */
    @PutMapping("/{id}/assign-driver")
    public ResponseEntity<TripResponse> assignDriver(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverRequest request) {
        
        log.info("PUT request to assign driver to trip: {}", id);
        
        Trip trip = tripService.assignDriverToTrip(id, request.getVehicleType());
        TripResponse response = TripResponse.from(trip);
        
        log.info("Driver assigned to trip {} successfully", id);
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // STATUS UPDATES
    // =========================================================================
    
    /**
     * Update trip status.
     * 
     * STATE MACHINE TRANSITIONS:
     * REQUESTED → ASSIGNED → STARTED → COMPLETED
     *          ↓           ↓        ↓
     *       CANCELLED   CANCELLED CANCELLED
     * 
     * SIDE EFFECTS:
     * - COMPLETED → Creates payment, releases driver
     * - CANCELLED → Releases driver
     * 
     * @param id Trip ID
     * @param request Status update request
     * @return Updated trip
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripStatusRequest request) {
        
        log.info("PUT request to update trip {} status to: {}", id, request.getStatus());
        
        Trip trip = tripService.updateTripStatus(id, request.getStatus());
        TripResponse response = TripResponse.from(trip);
        
        log.info("Trip {} status updated successfully", id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Start trip (convenience endpoint).
     * 
     * Transition: ASSIGNED → STARTED
     * 
     * ACCESS: DRIVER role only
     */
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/start")
    public ResponseEntity<TripResponse> startTrip(@PathVariable UUID id) {
        log.info("POST request to start trip: {}", id);
        
        Trip trip = tripService.updateTripStatus(id, TripStatus.STARTED);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Complete trip (convenience endpoint).
     * 
     * Transition: STARTED → COMPLETED
     * Side effects: Payment creation, driver release
     */
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<TripResponse> completeTrip(@PathVariable UUID id) {
        log.info("POST request to complete trip: {}", id);
        
        Trip trip = tripService.updateTripStatus(id, TripStatus.COMPLETED);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel trip (convenience endpoint).
     * 
     * Can cancel from any non-terminal status
     * Side effects: Driver release
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable UUID id) {
        log.info("POST request to cancel trip: {}", id);
        
        Trip trip = tripService.updateTripStatus(id, TripStatus.CANCELLED);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // TRIP RETRIEVAL
    // =========================================================================
    
    /**
     * Get trip by ID.
     * 
     * ACCESS: DRIVER or CLIENT (trip participants only)
     * 
     * @param id Trip ID
     * @return Trip details
     */
    // @PreAuthorize("hasAnyRole('DRIVER', 'CLIENT')")
    @PreAuthorize("@tripSecurity.canAccessTrip(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable UUID id) {
        log.debug("GET request for trip ID: {}", id);
        
        Trip trip = tripService.getTripById(id);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get trip with full details (client, driver, payment).
     * 
     * @param id Trip ID
     * @return Trip with all relationships loaded
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<TripResponse> getTripWithDetails(@PathVariable UUID id) {
        log.debug("GET request for trip {} with full details", id);
        
        Trip trip = tripService.getTripWithDetails(id);
        TripResponse response = TripResponse.fromDetailed(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get client's trip history.
     * 
     * PAGINATION:
     * - Default: page=0, size=20, sort=createdAt,desc
     * - Example: /api/v1/trips/client/123?page=1&size=10
     * 
     * @param clientId Client ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sort Sort fields (format: field,direction)
     * @return Paginated trips
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<TripResponse>> getClientTrips(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        log.debug("GET request for client {} trips (page: {}, size: {})", 
                  clientId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, createSort(sort));
        Page<Trip> trips = tripService.getClientTrips(clientId, pageable);
        Page<TripResponse> response = trips.map(TripResponse::from);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get client's active trip.
     * 
     * @param clientId Client ID
     * @return Active trip (if exists)
     */
    @GetMapping("/client/{clientId}/active")
    public ResponseEntity<TripResponse> getClientActiveTrip(@PathVariable UUID clientId) {
        log.debug("GET request for client {} active trip", clientId);
        
        Trip trip = tripService.getClientActiveTrip(clientId);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get driver's trip history.
     * 
     * @param driverId Driver ID
     * @param page Page number
     * @param size Page size
     * @param sort Sort fields
     * @return Paginated trips
     */
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Page<TripResponse>> getDriverTrips(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        log.debug("GET request for driver {} trips (page: {}, size: {})", 
                  driverId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, createSort(sort));
        Page<Trip> trips = tripService.getDriverTrips(driverId, pageable);
        Page<TripResponse> response = trips.map(TripResponse::from);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get driver's active trip.
     * 
     * @param driverId Driver ID
     * @return Active trip (if exists)
     */
    @GetMapping("/driver/{driverId}/active")
    public ResponseEntity<TripResponse> getDriverActiveTrip(@PathVariable UUID driverId) {
        log.debug("GET request for driver {} active trip", driverId);
        
        Trip trip = tripService.getDriverActiveTrip(driverId);
        TripResponse response = TripResponse.from(trip);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get trips by status.
     * 
     * USE CASE: Admin dashboard showing all REQUESTED trips
     * 
     * @param status Trip status
     * @param page Page number
     * @param size Page size
     * @return Paginated trips
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TripResponse>> getTripsByStatus(
            @PathVariable TripStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET request for trips with status: {} (page: {}, size: {})", 
                  status, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Trip> trips = tripService.getTripsByStatus(status, pageable);
        Page<TripResponse> response = trips.map(TripResponse::from);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Count trips by status.
     * 
     * @param status Trip status
     * @return Count of trips
     */
    @GetMapping("/status/{status}/count")
    public ResponseEntity<Long> countTripsByStatus(@PathVariable TripStatus status) {
        log.debug("GET request for trip count by status: {}", status);
        
        long count = tripService.countTripsByStatus(status);
        return ResponseEntity.ok(count);
    }
    
    // =========================================================================
    // UTILITY METHODS
    // =========================================================================
    
    /**
     * Create Sort object from string array.
     * 
     * Format: ["field1,direction1", "field2,direction2"]
     * Example: ["createdAt,desc", "fare,asc"]
     * 
     * Valid sort fields: id, status, fare, createdAt, version
     */
    private Sort createSort(String[] sortParams) {
        // Valid sort fields for Trip entity
        Set<String> validFields = Set.of("id", "status", "fare", "createdAt", "version");
        
        Sort sort = Sort.unsorted();
        
        for (String sortParam : sortParams) {
            if (sortParam == null || sortParam.trim().isEmpty()) {
                continue;
            }
            
            String[] parts = sortParam.split(",");
            String field = parts[0].trim();
            
            // Validate field name
            if (!validFields.contains(field)) {
                throw new IllegalArgumentException("Invalid sort field: " + field + 
                    ". Valid fields are: " + validFields);
            }
            
            Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            
            sort = sort.and(Sort.by(direction, field));
        }
        
        return sort;
    }
}
