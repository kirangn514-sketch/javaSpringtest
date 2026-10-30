package com.example.studentapp.dto;

import com.example.studentapp.entity.Role;

/**
 * Public view of a user. It deliberately has no password or password hash.
 */
public record UserResponseDTO(Long id, String username, Role role) {
}
