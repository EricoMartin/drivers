package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.model.Trip;
import com.basebox.ridelite.domain.enums.TripStatus;
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
public class TripResponse {
    private UUID id;
    private UUID clientId;
    private String clientEmail;
    private UUID driverId;
    private String driverEmail;
    private TripStatus status;
    private BigDecimal fare;
    private LocalDateTime createdAt;
    
    // Optional: Payment info
    private UUID paymentId;
    private String paymentStatus;
    
    /**
     * Convert entity to DTO (basic info).
     */
    public static TripResponse from(Trip trip) {
        TripResponseBuilder builder = TripResponse.builder()
            .id(trip.getId())
            .clientId(trip.getClient().getId())
            .clientEmail(trip.getClient().getUser().getEmail())
            .status(trip.getStatus())
            .fare(trip.getFare())
            .createdAt(trip.getCreatedAt());
        
        // Driver might be null (for REQUESTED trips)
        if (trip.getDriver() != null) {
            builder.driverId(trip.getDriver().getId())
                   .driverEmail(trip.getDriver().getUser().getEmail());
        }
        
        return builder.build();
    }
    
    /**
     * Convert entity to DTO (with payment info).
     */
    public static TripResponse fromDetailed(Trip trip) {
        TripResponse response = from(trip);
        
        // Add payment info if available
        if (trip.getPayment() != null) {
            response.setPaymentId(trip.getPayment().getId());
            response.setPaymentStatus(trip.getPayment().getStatus().toString());
        }
        
        return response;
    }
}
