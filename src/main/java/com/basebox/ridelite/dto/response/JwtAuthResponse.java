package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JWT authentication response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {
    private String accessToken;
    private String tokenType;  // Always "Bearer"
    private UUID userId;
    private String email;
    private Role role;
}
