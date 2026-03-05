package com.basebox.ridelite.repository;

import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.domain.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Trip entity.
 * 
 * This is your state machine repository - handles trip lifecycle.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    
    // =========================================================================
    // STATUS QUERIES (State Machine Queries)
    // =========================================================================
    
    /**
     * Find all trips with a specific status.
     */
    List<Trip> findByStatus(TripStatus status);
    
    /**
     * Find trips by status with pagination.
     * 
     * Use case: Admin panel showing "All COMPLETED trips" (page by page)
     */
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);
    
    /**
     * Find all REQUESTED trips (waiting for driver assignment).
     * 
     * Use case: Background job that assigns drivers to pending trips.
     */
    List<Trip> findByStatusOrderByCreatedAtAsc(TripStatus status);
    
    /**
     * Count trips by status.
     * 
     * Use case: Dashboard showing "5 active trips, 23 completed today"
     */
    long countByStatus(TripStatus status);
    
    // =========================================================================
    // CLIENT QUERIES
    // =========================================================================
    
    /**
     * Find all trips for a client.
     */
    List<Trip> findByClientId(UUID clientId);
    
    /**
     * Find client trips with pagination and sorting.
     * 
     * Use case: Client's trip history (newest first).
     */
    Page<Trip> findByClientIdOrderByCreatedAtDesc(UUID clientId, Pageable pageable);
    
    /**
     * Find client's active trip (if any).
     * 
     * Use case: "You already have an active trip"
     * 
     * A client shouldn't have multiple active trips simultaneously!
     */
    @Query("SELECT t FROM Trip t WHERE t.client.id = :clientId " +
           "AND t.status IN ('REQUESTED', 'ASSIGNED', 'STARTED')")
    Optional<Trip> findActiveTripByClientId(@Param("clientId") UUID clientId);
    
    /**
     * Check if client has any active trips.
     */
    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.client.id = :clientId " +
           "AND t.status IN ('REQUESTED', 'ASSIGNED', 'STARTED')")
    boolean hasActiveTripForClient(@Param("clientId") UUID clientId);
    
    /**
     * Find client trips with driver and user details (optimized).
     * 
     * Use case: Trip history showing driver names.
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.client c " +
           "JOIN FETCH c.user " +
           "LEFT JOIN FETCH t.driver d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE t.client.id = :clientId " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findByClientIdWithDetails(@Param("clientId") UUID clientId);
    
    // =========================================================================
    // DRIVER QUERIES
    // =========================================================================
    
    /**
     * Find all trips for a driver.
     */
    List<Trip> findByDriverId(UUID driverId);
    
    /**
     * Find driver trips with pagination.
     */
    Page<Trip> findByDriverIdOrderByCreatedAtDesc(UUID driverId, Pageable pageable);
    
    /**
     * Find driver's active trip (if any).
     * 
     * A driver should only have one active trip at a time!
     */
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId " +
           "AND t.status IN ('ASSIGNED', 'STARTED')")
    Optional<Trip> findActiveTripByDriverId(@Param("driverId") UUID driverId);
    
    /**
     * Check if driver has any active trips.
     */
    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.driver.id = :driverId " +
           "AND t.status IN ('ASSIGNED', 'STARTED')")
    boolean hasActiveTripForDriver(@Param("driverId") UUID driverId);
    
    /**
     * Find driver's completed trips with fare totals.
     */
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId " +
           "AND t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findCompletedTripsByDriver(@Param("driverId") UUID driverId);
    
    // =========================================================================
    // LOCKING QUERIES (State Transitions)
    // =========================================================================
    
    /**
     * Find trip by ID with pessimistic lock.
     * 
     * Use case: Updating trip status (prevent concurrent updates).
     * 
     * Example flow:
     * 1. Driver clicks "Start Trip"
     * 2. Lock the trip row
     * 3. Check current status is ASSIGNED
     * 4. Update to STARTED
     * 5. Commit (releases lock)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdWithLock(@Param("id") UUID id);
    
    /**
     * Find trip with all relationships loaded (for processing).
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.client c " +
           "JOIN FETCH c.user cu " +
           "LEFT JOIN FETCH t.driver d " +
           "LEFT JOIN FETCH d.user du " +
           "LEFT JOIN FETCH t.payment " +
           "WHERE t.id = :id")
    Optional<Trip> findByIdWithFullDetails(@Param("id") UUID id);
    
    // =========================================================================
    // ANALYTICS & REPORTING QUERIES
    // =========================================================================
    
    /**
     * Calculate total earnings for a driver.
     */
    @Query("SELECT SUM(t.fare) FROM Trip t " +
           "WHERE t.driver.id = :driverId " +
           "AND t.status = 'COMPLETED'")
    BigDecimal calculateDriverEarnings(@Param("driverId") UUID driverId);
    
    /**
     * Calculate driver earnings for a date range.
     */
    @Query("SELECT SUM(t.fare) FROM Trip t " +
           "WHERE t.driver.id = :driverId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal calculateDriverEarningsForPeriod(
        @Param("driverId") UUID driverId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    /**
     * Find top earning drivers (native SQL for complex aggregation).
     */
    @Query(value = """
        SELECT 
            d.id,
            u.email,
            COUNT(t.id) as trip_count,
            SUM(t.fare) as total_earnings
        FROM trips t
        JOIN drivers d ON t.driver_id = d.id
        JOIN users u ON d.user_id = u.id
        WHERE t.status = 'COMPLETED'
        GROUP BY d.id, u.email
        ORDER BY total_earnings DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopEarningDrivers(@Param("limit") int limit);
    
    /**
     * Get trip statistics by status.
     */
    @Query(value = """
        SELECT 
            status,
            COUNT(*) as count,
            AVG(fare) as avg_fare
        FROM trips
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getTripStatisticsByStatus();
    
    /**
     * Find trips created in the last N hours.
     */
    @Query("SELECT t FROM Trip t " +
           "WHERE t.createdAt > :cutoffTime " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findRecentTrips(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find stale REQUESTED trips (older than X minutes, not assigned).
     * 
     * Use case: Background job to handle abandoned trip requests.
     */
    @Query("SELECT t FROM Trip t " +
           "WHERE t.status = 'REQUESTED' " +
           "AND t.createdAt < :cutoffTime")
    List<Trip> findStaleRequestedTrips(@Param("cutoffTime") LocalDateTime cutoffTime);
}
