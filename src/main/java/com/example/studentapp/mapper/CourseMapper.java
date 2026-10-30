package com.example.studentapp.mapper;

import com.example.studentapp.dto.CourseRequestDTO;
import com.example.studentapp.dto.CourseResponseDTO;
import com.example.studentapp.dto.TechStackResponseDTO;
import com.example.studentapp.entity.Course;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Copies data between course DTOs and the Course entity.
 *
 * The response lists the linked tech stacks and the ids of the enrolled students.
 * This must run inside a transaction, because the course's collections are loaded lazily.
 */
@Component
public class CourseMapper {

    public Course toEntity(CourseRequestDTO request) {
        Course course = new Course();
        course.setName(request.name());
        course.setDescription(request.description());
        return course;
    }

    public CourseResponseDTO toResponseDTO(Course course) {
        List<TechStackResponseDTO> techStacks = course.getTechStacks().stream()
                .map(techStack -> new TechStackResponseDTO(techStack.getId(), techStack.getName()))
                .toList();
        List<Long> studentIds = course.getStudents().stream()
                .map(student -> student.getId())
                .toList();
        return new CourseResponseDTO(
                course.getId(),
                course.getName(),
                course.getDescription(),
                techStacks,
                studentIds
        );
    }

    public void updateEntity(Course course, CourseRequestDTO request) {
        course.setName(request.name());
        course.setDescription(request.description());
    }
}
