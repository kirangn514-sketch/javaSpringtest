package com.example.studentapp.exception;

/**
 * Thrown on registration when the username is already taken. Maps to 409 Conflict.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Username is already taken");
    }
}
