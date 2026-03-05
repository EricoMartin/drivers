package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.model.Payment;
import com.basebox.ridelite.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID tripId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    
    // Optional: Trip details
    private UUID clientId;
    private UUID driverId;
    
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .tripId(payment.getTrip().getId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .createdAt(payment.getCreatedAt())
            .build();
    }
    
    public static PaymentResponse fromDetailed(Payment payment) {
        PaymentResponse response = from(payment);
        response.setClientId(payment.getTrip().getClient().getId());
        if (payment.getTrip().getDriver() != null) {
            response.setDriverId(payment.getTrip().getDriver().getId());
        }
        return response;
    }
}