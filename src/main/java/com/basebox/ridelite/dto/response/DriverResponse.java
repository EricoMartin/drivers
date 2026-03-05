package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.model.Driver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private UUID id;
    private UUID userId;
    private String email;  // From user
    private Boolean isAvailable;
    private String vehicleType;
    private LocalDateTime createdAt;
    
    public static DriverResponse from(Driver driver) {
        return DriverResponse.builder()
            .id(driver.getId())
            .userId(driver.getUser().getId())
            .email(driver.getUser().getEmail())
            .isAvailable(driver.getIsAvailable())
            .vehicleType(driver.getVehicleType())
            .createdAt(driver.getCreatedAt())
            .build();
    }
}
