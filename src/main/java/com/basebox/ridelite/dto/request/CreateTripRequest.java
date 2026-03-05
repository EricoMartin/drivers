package com.basebox.ridelite.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {
    
    @NotNull(message = "Client ID is required")
    private UUID clientId;
    
    @NotNull(message = "Fare is required")
    @DecimalMin(value = "0.01", message = "Fare must be greater than 0")
    private BigDecimal fare;
    
    // Optional: specific vehicle type request
    private String vehicleType;
}
