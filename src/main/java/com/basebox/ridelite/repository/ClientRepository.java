package com.basebox.ridelite.repository;

import com.basebox.ridelite.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Client entity.
 * 
 * Simpler than Driver because clients don't have availability concerns.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    
    /**
     * Find client by user ID.
     */
    Optional<Client> findByUserId(UUID userId);
    
    /**
     * Find client with user details loaded.
     * 
     * Use case: Display client info (name, email) in admin panel.
     */
    @Query("SELECT c FROM Client c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Client> findByIdWithUser(@Param("id") UUID id);
    
    /**
     * Find client by user ID with all trips.
     * 
     * Use case: Client's trip history page.
     * 
     * WARNING: Can be slow if client has many trips!
     * Better: Use pagination (see TripRepository)
     */
    @Query("SELECT c FROM Client c " +
           "LEFT JOIN FETCH c.trips " +
           "WHERE c.user.id = :userId")
    Optional<Client> findByUserIdWithTrips(@Param("userId") UUID userId);
    
    /**
     * Check if user already has a client profile.
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Count total clients.
     */
    @Query("SELECT COUNT(c) FROM Client c")
    long countTotalClients();
}
