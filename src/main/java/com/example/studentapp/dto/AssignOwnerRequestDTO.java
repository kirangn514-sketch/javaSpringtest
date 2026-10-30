package com.example.studentapp.dto;

/**
 * Body for PUT /api/v1/students/{id}/owner (admin only).
 * Send the user id to link the student to that user. Send null to remove the link.
 */
public record AssignOwnerRequestDTO(Long userId) {
}
