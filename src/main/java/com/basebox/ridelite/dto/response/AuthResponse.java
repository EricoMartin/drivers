package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private Role role;
    private String token;  // Future: JWT token
    private String message;
}
