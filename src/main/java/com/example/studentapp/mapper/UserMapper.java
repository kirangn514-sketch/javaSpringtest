package com.example.studentapp.mapper;

import com.example.studentapp.dto.UserResponseDTO;
import com.example.studentapp.entity.AppUser;
import org.springframework.stereotype.Component;

/**
 * Converts a user entity to the public response. The password hash is never copied.
 */
@Component
public class UserMapper {

    public UserResponseDTO toResponseDTO(AppUser user) {
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getRole());
    }
}
