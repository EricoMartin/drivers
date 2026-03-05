package com.basebox.ridelite.repository;

import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    // PART 1: DERIVED QUERY METHODS (Method Name Conventions)
    //=========================================================

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);
    
    boolean existsByEmail(String email);

    List<User> findByCreatedAtAfter(LocalDateTime date);

    Page<User> findByRole(Role role, Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<User> findByRoleAndEmailContaining(Role role, String emailPart);

    //PART 2: Custom query example - (When Method Names Get Too Long)
    //================================================================

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.driver WHERE u.email = :email")
    Optional<User> findByEmailWithDriver(@Param("email") String email);

    /**
     * Find user by email with their client profile eagerly loaded.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.client WHERE u.email = :email")
    Optional<User> findByEmailWithClient(@Param("email") String email);

    /**
     * Find all users of a specific role with their profiles loaded.
     * 
     * DISTINCT is crucial here!
     * Without DISTINCT: If a driver has 5 trips, you get the same user 5 times
     * With DISTINCT: You get each user once
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.driver d " +
           "LEFT JOIN FETCH u.client c " +
           "WHERE u.role = :role")
    List<User> findByRoleWithProfiles(@Param("role") Role role);

    /**
     * Count users by role (more efficient than fetching all).
     * 
     * Generated SQL: SELECT COUNT(*) FROM users WHERE role = ?
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);

    /**
     * Find users created in the last N days.
     * 
     * Uses JPQL function CURRENT_DATE for database-agnostic date comparison.
     */
    @Query("SELECT u FROM User u WHERE u.createdAt > FUNCTION('DATE_SUB', CURRENT_DATE, :days)")
    List<User> findRecentUsers(@Param("days") int days);

    // Part 3: NATIVE SQL QUERIES (When JPQL Isn't Enough)
    //=========================================================

    /**
     * Complex statistics query using native SQL.
     * 
     * WHEN TO USE NATIVE SQL:
     * - Database-specific functions (PostgreSQL's EXTRACT, MySQL's DATE_FORMAT)
     * - Complex aggregations
     * - Performance-critical queries
     * - Using database-specific features
     * 
     * TRADE-OFF:
     * ✅ More control, can use DB-specific features
     * ❌ Not portable across databases
     */
    @Query(value = "SELECT role, COUNT(*) as count FROM users GROUP BY role", 
           nativeQuery = true)
    List<Object[]> getUserRoleStatistics();
    
    /**
     * Find users with pagination using native SQL.
     * 
     * Spring automatically handles pagination in native queries!
     */
    @Query(value = "SELECT * FROM users WHERE role = :role ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM users WHERE role = :role",
           nativeQuery = true)
    Page<User> findByRoleNative(@Param("role") String role, Pageable pageable);

}
