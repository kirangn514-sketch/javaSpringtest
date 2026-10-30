package com.example.studentapp.dto;

import com.example.studentapp.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for PUT /api/v1/users/{id} (admin only).
 * The password is optional: send null or leave it out to keep the current password.
 */
public record UpdateUserRequestDTO(

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,

        // Optional. @Size ignores null, so a missing password is allowed.
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "role is required (ADMIN, HR, or EMPLOYEE)")
        Role role
) {
}
