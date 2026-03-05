package com.basebox.ridelite.controller;

import com.basebox.ridelite.domain.model.Payment;
import com.basebox.ridelite.domain.enums.PaymentStatus;
import com.basebox.ridelite.dto.response.PaymentResponse;
import com.basebox.ridelite.dto.response.RevenueResponse;
import com.basebox.ridelite.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for payment operations.
 * 
 * ENDPOINTS:
 * - GET  /api/v1/payments/{id}              → Get payment by ID
 * - GET  /api/v1/payments/trip/{tripId}     → Get payment by trip ID
 * - POST /api/v1/payments/{id}/process      → Process payment (retry)
 * - GET  /api/v1/payments/revenue           → Get total revenue
 * - GET  /api/v1/payments/revenue/period    → Get revenue for period
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PaymentController {
    
    private final PaymentService paymentService;
    
    // =========================================================================
    // PAYMENT RETRIEVAL
    // =========================================================================
    
    /**
     * Get payment by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        log.debug("GET request for payment ID: {}", id);
        
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = PaymentResponse.from(payment);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get payment by trip ID.
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<PaymentResponse> getPaymentByTripId(@PathVariable UUID tripId) {
        log.debug("GET request for payment by trip ID: {}", tripId);
        
        Payment payment = paymentService.getPaymentByTripId(tripId);
        PaymentResponse response = PaymentResponse.from(payment);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get payment with full details.
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<PaymentResponse> getPaymentWithDetails(@PathVariable UUID id) {
        log.debug("GET request for payment {} with full details", id);
        
        Payment payment = paymentService.getPaymentWithDetails(id);
        PaymentResponse response = PaymentResponse.fromDetailed(payment);
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // PAYMENT PROCESSING
    // =========================================================================
    
    /**
     * Process/retry payment.
     * 
     * USE CASE: 
     * - Retry failed payment
     * - Manual payment trigger
     * 
     * @param id Payment ID
     * @return Processed payment
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable UUID id) {
        log.info("POST request to process payment: {}", id);
        
        Payment payment = paymentService.processPayment(id);
        PaymentResponse response = PaymentResponse.from(payment);
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================================
    // REVENUE STATISTICS
    // =========================================================================
    
    /**
     * Get total revenue (all PAID payments).
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueResponse> getTotalRevenue() {
        log.debug("GET request for total revenue");
        
        BigDecimal revenue = paymentService.getTotalRevenue();
        
        RevenueResponse response = RevenueResponse.builder()
            .totalRevenue(revenue)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get revenue for a specific period.
     * 
     * QUERY PARAMS:
     * - start: Start date (ISO format: 2024-01-01T00:00:00)
     * - end: End date
     * 
     * Example: /api/v1/payments/revenue/period?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59
     */
    @GetMapping("/revenue/period")
    public ResponseEntity<RevenueResponse> getRevenueForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        log.debug("GET request for revenue from {} to {}", start, end);
        
        BigDecimal revenue = paymentService.getRevenueForPeriod(start, end);
        
        RevenueResponse response = RevenueResponse.builder()
            .totalRevenue(revenue)
            .startDate(start)
            .endDate(end)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Count payments by status.
     */
    @GetMapping("/status/{status}/count")
    public ResponseEntity<Long> countPaymentsByStatus(@PathVariable PaymentStatus status) {
        log.debug("GET request for payment count by status: {}", status);
        
        long count = paymentService.countPaymentsByStatus(status);
        return ResponseEntity.ok(count);
    }
}