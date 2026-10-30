package com.example.studentapp.exception;

/**
 * Thrown when a course with the requested ID does not exist. Maps to 404 COURSE_NOT_FOUND.
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }
}
