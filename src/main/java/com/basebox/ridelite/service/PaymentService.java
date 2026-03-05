package com.basebox.ridelite.service;

import com.basebox.ridelite.domain.model.Payment;
import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.domain.enums.PaymentStatus;
import com.basebox.ridelite.exception.InvalidOperationException;
import com.basebox.ridelite.exception.PaymentException;
import com.basebox.ridelite.exception.ResourceNotFoundException;
import com.basebox.ridelite.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for Payment processing (simulated).
 * 
 * In production, this would integrate with:
 * - Stripe, PayPal, Square, etc.
 * - Payment gateway APIs
 * - Webhook handlers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    // =========================================================================
    // PAYMENT CREATION
    // =========================================================================
    
    /**
     * Create payment for a completed trip.
     * 
     * BUSINESS RULES:
     * 1. One payment per trip
     * 2. Payment amount = trip fare
     * 3. Starts in PENDING status
     * 
     * @param trip Completed trip
     * @return Created payment
     * @throws InvalidOperationException if payment already exists
     */
    @Transactional
    public Payment createPaymentForTrip(Trip trip) {
        log.info("Creating payment for trip: {}", trip.getId());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 1: Check if payment already exists
        // ─────────────────────────────────────────────────────────────────
        if (paymentRepository.findByTripId(trip.getId()).isPresent()) {
            log.warn("Payment already exists for trip: {}", trip.getId());
            throw new InvalidOperationException(
                "Payment already exists for this trip"
            );
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 2: Validate trip has fare
        // ─────────────────────────────────────────────────────────────────
        if (trip.getFare() == null || trip.getFare().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Cannot create payment: Trip {} has invalid fare", trip.getId());
            throw new InvalidOperationException("Trip has invalid fare amount");
        }
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 3: Create payment
        // ─────────────────────────────────────────────────────────────────
        Payment payment = Payment.builder()
            .trip(trip)
            .amount(trip.getFare())
            .status(PaymentStatus.PENDING)
            .build();
        
        payment = paymentRepository.save(payment);
        
        log.info("Payment created successfully with ID: {}", payment.getId());
        
        // ─────────────────────────────────────────────────────────────────
        // STEP 4: Attempt to process payment immediately
        // ─────────────────────────────────────────────────────────────────
        try {
            payment = processPayment(payment.getId());
        } catch (PaymentException e) {
            log.error("Immediate payment processing failed: {}", e.getMessage());
            // Payment remains in PENDING - retry job will handle it
        }
        
        return payment;
    }
    
    // =========================================================================
    // PAYMENT PROCESSING (Simulated)
    // =========================================================================
    
    /**
     * Process payment (simulated payment gateway call).
     * 
     * PRODUCTION IMPLEMENTATION:
     * - Call Stripe/PayPal API
     * - Handle webhooks
     * - Implement idempotency
     * - Add retry logic
     * 
     * CURRENT IMPLEMENTATION:
     * - 90% success rate (simulated)
     * - Updates status to PAID or FAILED
     * 
     * @param paymentId Payment ID
     * @return Processed payment
     * @throws PaymentException if processing fails
     */
    @Transactional
    public Payment processPayment(UUID paymentId) {
        log.info("Processing payment: {}", paymentId);
        
        Payment payment = getPaymentById(paymentId);
        
        // ─────────────────────────────────────────────────────────────────
        // BUSINESS RULE: Only process PENDING payments
        // ─────────────────────────────────────────────────────────────────
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Cannot process payment {} in {} status", 
                     paymentId, payment.getStatus());
            throw new InvalidOperationException(
                "Can only process payments in PENDING status"
            );
        }
        
        try {
            // ─────────────────────────────────────────────────────────────
            // SIMULATE: Payment gateway call
            // ─────────────────────────────────────────────────────────────
            boolean success = simulatePaymentGateway(payment);
            
            if (success) {
                payment.setStatus(PaymentStatus.PAID);
                log.info("Payment {} processed successfully", paymentId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.warn("Payment {} processing failed", paymentId);
                throw new PaymentException("Payment gateway declined the transaction");
            }
            
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            log.error("Payment {} processing error: {}", paymentId, e.getMessage());
            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        } finally {
            payment = paymentRepository.save(payment);
        }
        
        return payment;
    }
    
    /**
     * Simulate payment gateway (90% success rate).
     * 
     * PRODUCTION: Replace with actual API call
     */
    private boolean simulatePaymentGateway(Payment payment) {
        log.debug("Simulating payment gateway for payment: {}", payment.getId());
        
        // Simulate processing time
        try {
            Thread.sleep(500);  // 500ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 90% success rate
        return Math.random() < 0.9;
    }
    
    // =========================================================================
    // PAYMENT RETRIEVAL
    // =========================================================================
    
    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID paymentId) {
        log.debug("Fetching payment by ID: {}", paymentId);
        
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId.toString()));
    }
    
    /**
     * Get payment by trip ID.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByTripId(UUID tripId) {
        log.debug("Fetching payment by trip ID: {}", tripId);
        
        return paymentRepository.findByTripId(tripId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Payment for trip", tripId.toString()
            ));
    }
    
    /**
     * Get payment with full details.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentWithDetails(UUID paymentId) {
        log.debug("Fetching payment with details: {}", paymentId);
        
        return paymentRepository.findByIdWithDetails(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId.toString()));
    }
    
    // =========================================================================
    // PAYMENT RETRY (For Failed Payments)
    // =========================================================================
    
    /**
     * Retry failed payments.
     * 
     * USE CASE: Background job retries failed payments every 5 minutes
     * 
     * @param minutesOld Retry payments older than this
     * @return List of retry results
     */
    @Transactional
    public List<Payment> retryFailedPayments(int minutesOld) {
        log.info("Retrying failed payments older than {} minutes", minutesOld);
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesOld);
        List<Payment> failedPayments = paymentRepository.findFailedPaymentsToRetry(cutoff);
        
        log.info("Found {} failed payments to retry", failedPayments.size());
        
        for (Payment payment : failedPayments) {
            try {
                // Reset to PENDING before retry
                payment.setStatus(PaymentStatus.PENDING);
                paymentRepository.save(payment);
                
                // Attempt to process
                processPayment(payment.getId());
                
            } catch (PaymentException e) {
                log.error("Retry failed for payment {}: {}", 
                         payment.getId(), e.getMessage());
                // Payment remains in FAILED status
            }
        }
        
        return failedPayments;
    }
    
    // =========================================================================
    // STATISTICS
    // =========================================================================
    
    /**
     * Calculate total revenue.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = paymentRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    /**
     * Calculate revenue for period.
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = paymentRepository.calculateRevenueForPeriod(start, end);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    /**
     * Count payments by status.
     */
    @Transactional(readOnly = true)
    public long countPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.countByStatus(status);
    }
}
