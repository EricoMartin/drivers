package com.basebox.ridelite.dto.request;

import com.basebox.ridelite.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {

    
    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "firstName is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "lastName is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    // Only for DRIVER role
    private String vehicleType;
}
