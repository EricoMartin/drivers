package com.basebox.ridelite.repository;

import com.basebox.ridelite.domain.model.Payment;
import com.basebox.ridelite.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    /**
     * Find payment by trip ID.
     * 
     * One-to-one relationship: each trip has exactly one payment.
     */
    Optional<Payment> findByTripId(UUID tripId);
    
    /**
     * Find payments by status.
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find payments by status with pagination.
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    
    /**
     * Find all PENDING payments (for retry processing).
     * 
     * Use case: Background job that retries failed payments.
     */
    List<Payment> findByStatusOrderByCreatedAtAsc(PaymentStatus status);
    
    /**
     * Find failed payments older than a certain time.
     * 
     * Use case: Retry payments that failed over 5 minutes ago.
     */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.status = 'FAILED' " +
           "AND p.createdAt < :cutoffTime")
    List<Payment> findFailedPaymentsToRetry(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find payment with trip and client details.
     * 
     * Use case: Payment receipt showing trip and client info.
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.trip t " +
           "JOIN FETCH t.client c " +
           "JOIN FETCH c.user " +
           "WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") UUID id);
    
    /**
     * Count payments by status.
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Calculate total revenue (all PAID payments).
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    BigDecimal calculateTotalRevenue();
    
    /**
     * Calculate revenue for a date range.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'PAID' " +
           "AND p.createdAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueForPeriod(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    /**
     * Get payment statistics.
     */
    @Query(value = """
        SELECT 
            status,
            COUNT(*) as count,
            SUM(amount) as total_amount,
            AVG(amount) as avg_amount
        FROM payments
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getPaymentStatistics();
}