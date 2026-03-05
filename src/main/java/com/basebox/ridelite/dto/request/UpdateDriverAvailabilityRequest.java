package com.basebox.ridelite.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverAvailabilityRequest {
    
    @NotNull(message = "Availability status is required")
    private Boolean available;
}
