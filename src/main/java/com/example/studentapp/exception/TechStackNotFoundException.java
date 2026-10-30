package com.example.studentapp.exception;

/**
 * Thrown when a tech stack with the requested ID does not exist. Maps to 404 TECH_STACK_NOT_FOUND.
 */
public class TechStackNotFoundException extends RuntimeException {

    public TechStackNotFoundException(Long id) {
        super("Tech stack not found with id: " + id);
    }
}
