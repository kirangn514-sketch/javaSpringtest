package com.example.studentapp.security;

import com.example.studentapp.entity.Student;
import com.example.studentapp.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Ownership check used by @PreAuthorize on GET /api/v1/students/{id}.
 *
 * Spring calls it from an expression: @studentAccess.isOwner(#id, authentication).
 * The bean name "studentAccess" comes from the @Component value below.
 * It returns false when the student doesn't exist, so an employee gets 403 rather than learning
 * whether an ID exists.
 */
@Component("studentAccess")
public class StudentAccess {

    private final StudentRepository studentRepository;

    public StudentAccess(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /** True if the student's owner has the same username as the logged-in user. */
    public boolean isOwner(Long studentId, Authentication authentication) {
        return studentRepository.findById(studentId)
                .map(Student::getOwner)
                .map(owner -> owner.getUsername().equals(authentication.getName()))
                .orElse(false);
    }
}
