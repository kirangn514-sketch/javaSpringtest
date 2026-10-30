package com.example.studentapp.exception;

/**
 * Thrown when a user with the requested ID does not exist. Maps to 404 USER_NOT_FOUND.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
