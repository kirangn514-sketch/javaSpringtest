package com.example.studentapp.exception;

/**
 * Thrown when a course or tech stack name is already used by another record. Maps to 409 NAME_TAKEN.
 */
public class DuplicateNameException extends RuntimeException {

    public DuplicateNameException(String resource) {
        super(resource + " name is already taken");
    }
}
