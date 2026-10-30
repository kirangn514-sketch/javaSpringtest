package com.example.studentapp.dto;

import java.time.Instant;

/**
 * Standard wrapper for every API response, required by the organization's API standard:
 *   {"success": true, "data": {...}, "message": "...", "errorCode": null, "timestamp": "..."}
 *
 * One shape for success and failure means clients can always read the same fields.
 * The generic type T is the type of "data". For example, ApiResponse<StudentResponseDTO>.
 * Failures use error(...), which sets success to false and leaves data empty.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, Instant.now());
    }
}
