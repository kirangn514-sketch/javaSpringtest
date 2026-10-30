package com.example.studentapp.dto;

/**
 * Data that the API sends back to the client (the JSON response body).
 *
 * This is the only shape clients see. The Student entity is never returned, so internal
 * database fields such as createdAt stay private, and renaming a column doesn't break the API.
 *
 * ownerUserId is the id of the user the record is linked to, or null if it has no owner.
 */
public record StudentResponseDTO(
        Long id,
        String name,
        String subject,
        String address,
        Integer age,
        Long ownerUserId
) {
}
