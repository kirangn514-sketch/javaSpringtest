package com.example.studentapp.controller;

import com.example.studentapp.dto.ApiResponse;
import com.example.studentapp.dto.CourseRequestDTO;
import com.example.studentapp.dto.CourseResponseDTO;
import com.example.studentapp.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Course endpoints, including the links to tech stacks and students. ADMIN only.
 *
 *   POST   /courses/{id}/tech-stacks/{techStackId}  link a tech stack
 *   DELETE /courses/{id}/tech-stacks/{techStackId}  unlink a tech stack
 *   POST   /courses/{id}/students/{studentId}       enroll a student
 *   DELETE /courses/{id}/students/{studentId}       remove a student
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success(courseService.getAllCourses(), "Courses retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourseById(id), "Course retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponseDTO>> createCourse(@Valid @RequestBody CourseRequestDTO request) {
        CourseResponseDTO created = courseService.createCourse(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(created, "Course created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.updateCourse(id, request), "Course updated successfully"));
    }

    /** DELETE /api/v1/courses/{id} returns 204. The course's mapping rows are removed with it. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tech-stacks/{techStackId}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> addTechStack(
            @PathVariable Long id,
            @PathVariable Long techStackId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.addTechStack(id, techStackId), "Tech stack added to course"));
    }

    @DeleteMapping("/{id}/tech-stacks/{techStackId}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> removeTechStack(
            @PathVariable Long id,
            @PathVariable Long techStackId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.removeTechStack(id, techStackId), "Tech stack removed from course"));
    }

    @PostMapping("/{id}/students/{studentId}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> addStudent(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.addStudent(id, studentId), "Student enrolled in course"));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.removeStudent(id, studentId), "Student removed from course"));
    }
}
