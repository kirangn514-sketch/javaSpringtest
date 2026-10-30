package com.example.studentapp.mapper;

import com.example.studentapp.dto.StudentRequestDTO;
import com.example.studentapp.dto.StudentResponseDTO;
import com.example.studentapp.entity.Student;
import org.springframework.stereotype.Component;

/**
 * Copies data between DTOs and the Student entity.
 *
 * Mapping is kept out of the Controller and the Service so that each layer has one job:
 *   - the Controller handles HTTP
 *   - the Service handles business rules
 *   - the Mapper handles conversion between shapes
 *
 * @Component registers this class as a Spring bean. The Service receives it through its constructor,
 * the same way it receives the Repository. The mapper has no dependencies of its own,
 * and it keeps no state, so one shared instance is safe.
 */
@Component
public class StudentMapper {

    /** Builds a new, unsaved entity from the request. The id stays null, so the database assigns one on save. */
    public Student toEntity(StudentRequestDTO request) {
        Student student = new Student();
        student.setName(request.name());
        student.setSubject(request.subject());
        student.setAddress(request.address());
        student.setAge(request.age());
        return student;
    }

    /** Builds the response from a saved entity. It copies only the fields the API exposes. */
    public StudentResponseDTO toResponseDTO(Student student) {
        Long ownerUserId = student.getOwner() == null ? null : student.getOwner().getId();
        return new StudentResponseDTO(
                student.getId(),
                student.getName(),
                student.getSubject(),
                student.getAddress(),
                student.getAge(),
                ownerUserId
        );
    }

    /**
     * Copies the request values onto an existing entity, which is then saved as an UPDATE.
     * The entity's id, owner, and createdAt are not touched, so the row stays the same row.
     */
    public void updateEntity(Student student, StudentRequestDTO request) {
        student.setName(request.name());
        student.setSubject(request.subject());
        student.setAddress(request.address());
        student.setAge(request.age());
    }
}
