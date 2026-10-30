package com.example.studentapp.dto;

import java.util.List;

/**
 * A course as the API returns it.
 *
 * techStacks lists the linked tech stacks with their ids and names.
 * studentIds lists the ids of enrolled students. Student details are fetched from /api/v1/students.
 */
public record CourseResponseDTO(
        Long id,
        String name,
        String description,
        List<TechStackResponseDTO> techStacks,
        List<Long> studentIds
) {
}
