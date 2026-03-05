package com.basebox.ridelite.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.basebox.ridelite.domain.model.Driver;

import jakarta.persistence.LockModeType;

/**
 * Repository for Driver entity.
 * 
 * CRITICAL: This repository handles concurrent driver assignments.
 * We use pessimistic locking to prevent race conditions.
 */

public interface DriverRepository extends JpaRepository<Driver, UUID>{
    // Add driver-specific repository methods here
    List<Driver> findByIsAvailableTrue();
    
    /**
     * Find available drivers by vehicle type.
     * 
     * Use case: "Find me available SUV drivers"
     */
    List<Driver> findByIsAvailableTrueAndVehicleType(String vehicleType);

    /**
     * Find available drivers with their user details (optimized).
     * 
     * WHY JOIN FETCH?
     * - Avoids N+1 problem
     * - Gets driver + user in ONE query
     * - Essential for displaying driver info (name, email, etc.)
     */
    @Query("SELECT d FROM Driver d " +
           "JOIN FETCH d.user " +
           "WHERE d.isAvailable = true")
    List<Driver> findAvailableDriversWithUser();
    
    /**
     * Find available drivers by vehicle type with user details.
     */
    @Query("SELECT d FROM Driver d " +
           "JOIN FETCH d.user " +
           "WHERE d.isAvailable = true " +
           "AND d.vehicleType = :vehicleType")
    List<Driver> findAvailableDriversByVehicleTypeWithUser(
        @Param("vehicleType") String vehicleType
    );
    
    // =========================================================================
    // LOCKING QUERIES (Preventing Race Conditions!)
    // =========================================================================
    
    /**
     * Find driver by ID with PESSIMISTIC WRITE lock.
     * 
     * WHAT IS PESSIMISTIC LOCKING?
     * Imagine a bathroom with one stall:
     * - Person A enters, LOCKS the door
     * - Person B tries to enter, WAITS until A is done
     * - No one can enter until the lock is released
     * 
     * DATABASE VERSION:
     * - Thread A reads driver, DB LOCKS that row
     * - Thread B tries to read same driver, WAITS
     * - When A finishes transaction, B can proceed
     * 
     * SQL Generated: 
     * SELECT * FROM drivers WHERE id = ? FOR UPDATE
     * 
     * The "FOR UPDATE" clause locks the row!
     * 
     * @param id Driver ID
     * @return Optional<Driver> with the row locked
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d WHERE d.id = :id")
    Optional<Driver> findByIdWithLock(@Param("id") UUID id);
    
    /**
     * Find available driver with lock (for assignment).
     * 
     * USE CASE: When assigning a driver to a trip
     * 
     * FLOW:
     * 1. Find first available driver
     * 2. Lock that row
     * 3. Assign to trip
     * 4. Update availability
     * 5. Release lock (transaction ends)
     * 
     * This prevents two trips from getting the same driver!
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d " +
           "WHERE d.isAvailable = true " +
           "ORDER BY d.createdAt ASC")
    Optional<Driver> findFirstAvailableDriverWithLock();
    
    /**
     * Find available driver by vehicle type with lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d " +
           "WHERE d.isAvailable = true " +
           "AND d.vehicleType = :vehicleType " +
           "ORDER BY d.createdAt ASC")
    Optional<Driver> findFirstAvailableDriverByVehicleTypeWithLock(
        @Param("vehicleType") String vehicleType
    );
    
    // =========================================================================
    // USER RELATIONSHIP QUERIES
    // =========================================================================
    
    /**
     * Find driver by user ID.
     * 
     * Use case: User logs in, you need their driver profile.
     */
    Optional<Driver> findByUserId(UUID userId);
    
    /**
     * Find driver by user ID with trips loaded.
     * 
     * Use case: Driver dashboard showing their trip history.
     */
    @Query("SELECT d FROM Driver d " +
           "LEFT JOIN FETCH d.trips " +
           "WHERE d.user.id = :userId")
    Optional<Driver> findByUserIdWithTrips(@Param("userId") UUID userId);
    
    /**
     * Check if a user already has a driver profile.
     */
    boolean existsByUserId(UUID userId);
    
    // =========================================================================
    // STATISTICS QUERIES
    // =========================================================================
    
    /**
     * Count available drivers.
     * 
     * Use case: Dashboard showing "15 drivers available"
     */
    long countByIsAvailableTrue();
    
    /**
     * Count available drivers by vehicle type.
     */
    long countByIsAvailableTrueAndVehicleType(String vehicleType);
    
    /**
     * Get driver statistics (native SQL for complex aggregation).
     */
    @Query(value = """
        SELECT 
            vehicle_type,
            COUNT(*) as total_drivers,
            SUM(CASE WHEN is_available THEN 1 ELSE 0 END) as available_drivers
        FROM drivers
        GROUP BY vehicle_type
        """, nativeQuery = true)
    List<Object[]> getDriverStatisticsByVehicleType();
}
