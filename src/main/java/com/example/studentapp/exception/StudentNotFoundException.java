package com.example.studentapp.exception;

/**
 * Thrown when a student with the requested ID does not exist.
 *
 * It extends RuntimeException, which is unchecked, so Java does not force every caller to catch it.
 * The Service throws it, and the GlobalExceptionHandler (Phase 8) turns it into a 404 response.
 * The message is built here so every place that throws it reports the same text.
 */
public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
    }
}
