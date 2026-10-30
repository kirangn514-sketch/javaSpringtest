package com.example.studentapp.controller;

import com.example.studentapp.dto.ApiResponse;
import com.example.studentapp.dto.AssignOwnerRequestDTO;
import com.example.studentapp.dto.StudentRequestDTO;
import com.example.studentapp.dto.StudentResponseDTO;
import com.example.studentapp.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * HTTP entry point for students. It receives requests, passes them to the Service,
 * and returns responses. It contains no business rules and no database code.
 *
 * Access rules: the URL rules in SecurityConfig decide which roles may reach each path.
 * GET /students/{id} also has a @PreAuthorize check, which lets an EMPLOYEE read only
 * the students they own.
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** POST /api/v1/students creates a student and returns 201 Created. ADMIN only. */
    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponseDTO>> createStudent(
            @Valid @RequestBody StudentRequestDTO request) {

        StudentResponseDTO created = studentService.createStudent(request);

        // Location tells the client where the new resource lives, e.g. http://localhost:8080/api/v1/students/1
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(created, "Student created successfully"));
    }

    /** GET /api/v1/students returns every student with 200 OK. ADMIN and HR. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponseDTO>>> getAllStudents() {
        List<StudentResponseDTO> students = studentService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(students, "Students retrieved successfully"));
    }

    /**
     * GET /api/v1/students/{id} returns one student with 200 OK.
     *
     * @PreAuthorize runs before the method. ADMIN and HR pass. An EMPLOYEE passes only when
     * StudentAccess.isOwner says the student belongs to them. Otherwise Spring throws
     * AccessDeniedException, which the GlobalExceptionHandler turns into 403.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @studentAccess.isOwner(#id, authentication)")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> getStudentById(@PathVariable Long id) {
        StudentResponseDTO student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success(student, "Student retrieved successfully"));
    }

    /** PUT /api/v1/students/{id} replaces the student's fields and returns 200 OK. ADMIN only. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO request) {

        StudentResponseDTO updated = studentService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Student updated successfully"));
    }

    /** DELETE /api/v1/students/{id} returns 204 No Content. ADMIN only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/students/{id}/owner links the student to a user, so that employee can read it.
     * Send {"userId": 3} to link, or {"userId": null} to unlink. ADMIN only.
     */
    @PutMapping("/{id}/owner")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> assignOwner(
            @PathVariable Long id,
            @RequestBody AssignOwnerRequestDTO request) {

        StudentResponseDTO updated = studentService.assignOwner(id, request.userId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Student owner updated successfully"));
    }
}
