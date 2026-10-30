package com.example.studentapp.dto;

import com.example.studentapp.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/users (admin only). The admin chooses the role.
 * Public self-registration always creates an EMPLOYEE, so it cannot be used to make an admin.
 */
public record CreateUserRequestDTO(

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "role is required (ADMIN, HR, or EMPLOYEE)")
        Role role
) {
}
