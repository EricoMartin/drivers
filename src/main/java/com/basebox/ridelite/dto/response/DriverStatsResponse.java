package com.basebox.ridelite.dto.response;

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
public class DriverStatsResponse {
    private UUID driverId;
    private long totalTrips;
    private BigDecimal totalEarnings;
    private BigDecimal averageFare;
    private Boolean isAvailable;
    private String vehicleType;
}
