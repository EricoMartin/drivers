package com.basebox.ridelite.dto.response;

import com.basebox.ridelite.domain.model.User;
import com.basebox.ridelite.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for User responses.
 * 
 * IMPORTANT: Never expose password field!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    
    /**
     * Convert entity to DTO.
     * 
     * PATTERN: Static factory method
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
