package com.example.studentapp.exception;

/**
 * Thrown for a wrong username or a wrong password. Both cases share one message on purpose,
 * so an attacker can't use the response to find out which usernames exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
