package com.example.studentapp.exception;

import com.example.studentapp.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * One place that turns exceptions into the standard error response.
 *
 * @RestControllerAdvice applies to every controller. When a controller method throws an exception
 * that nothing else catches, Spring looks for a matching @ExceptionHandler method here.
 *
 * Flow when a request fails:
 *   Service throws StudentNotFoundException
 *     → nothing in the Controller catches it
 *     → Spring finds handleNotFound(...) in this class
 *     → the method returns 404 with an ApiResponse body
 *
 * Every error uses the same ApiResponse shape as success, with success = false and an errorCode
 * the client can check in code. The message is for humans and may change.
 *
 * Stack traces are logged on the server but never sent to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Missing student: 404 Not Found. */
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(StudentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "STUDENT_NOT_FOUND");
    }

    /**
     * @Valid failed on a request body: 400 Bad Request.
     * The message lists each bad field, e.g. "age: age must be at most 120; name: name is required".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, details, "VALIDATION_ERROR");
    }

    /** The body is not valid JSON, or does not match the DTO (for example, a missing body). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", "MALFORMED_REQUEST");
    }

    /** A path value has the wrong type, such as GET /api/v1/students/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, message, "INVALID_PARAMETER");
    }

    /**
     * Last resort for anything not handled above: 500.
     * The full exception goes to the log for developers. The client gets a generic message only,
     * so internal details like SQL or class names are not exposed.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_ERROR");
    }

    /** Wrong username or password: 401 Unauthorized. */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS");
    }

    /** Registration with a username that already exists: 409 Conflict. */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameTaken(UsernameAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "USERNAME_TAKEN");
    }

    /** Missing user: 404 Not Found. */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_FOUND");
    }

    /** Missing course: 404 Not Found. */
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseNotFound(CourseNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "COURSE_NOT_FOUND");
    }

    /** Missing tech stack: 404 Not Found. */
    @ExceptionHandler(TechStackNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechStackNotFound(TechStackNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "TECH_STACK_NOT_FOUND");
    }

    /** Course or tech stack name already used: 409 Conflict. */
    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateName(DuplicateNameException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "NAME_TAKEN");
    }

    /**
     * The user is logged in but the role is not allowed, or the owner check in @PreAuthorize failed: 403.
     * Without this handler, the catch-all below would turn it into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "You do not have permission for this action", "FORBIDDEN");
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message, String errorCode) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, errorCode));
    }
}
