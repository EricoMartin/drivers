package com.basebox.ridelite.dto.request;

import com.basebox.ridelite.domain.enums.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripStatusRequest {
    
    @NotNull(message = "Status is required")
    private TripStatus status;
}