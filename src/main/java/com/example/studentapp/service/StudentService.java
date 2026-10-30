package com.example.studentapp.service;

import com.example.studentapp.dto.StudentRequestDTO;
import com.example.studentapp.dto.StudentResponseDTO;
import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Student;
import com.example.studentapp.exception.StudentNotFoundException;
import com.example.studentapp.exception.UserNotFoundException;
import com.example.studentapp.mapper.StudentMapper;
import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for students. The Controller calls this class, and this class calls the Repository.
 *
 * Responsibilities:
 *   - decide what should happen (for example, "updating a missing student is an error")
 *   - coordinate the Repositories and the Mapper
 *   - return DTOs, never entities, to the Controller
 *
 * Constructor injection: Spring passes in the beans below. A unit test can pass in mocks instead.
 */
@Service
public class StudentService {

    /** SLF4J logger. It records events in the console without exposing the whole object. */
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final AppUserRepository appUserRepository;
    private final CourseRepository courseRepository;

    // No @Autowired needed: Spring Boot uses the only constructor automatically.
    public StudentService(StudentRepository studentRepository,
                          StudentMapper studentMapper,
                          AppUserRepository appUserRepository,
                          CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.appUserRepository = appUserRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * @Transactional wraps the method in a database transaction. Everything inside commits together,
     * or everything rolls back if an unchecked exception escapes the method.
     */
    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO request) {
        Student saved = studentRepository.save(studentMapper.toEntity(request));
        log.info("Created student with id={}", saved.getId());
        return studentMapper.toResponseDTO(saved);
    }

    /** readOnly = true tells the database and Hibernate that nothing will be written, which allows some optimizations. */
    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(studentMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        return studentMapper.toResponseDTO(findStudentOrThrow(id));
    }

    /**
     * Finds the existing row first, then copies new values onto it.
     * Because it is never a new Student, a missing id throws instead of creating a row.
     */
    @Transactional
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO request) {
        Student student = findStudentOrThrow(id);
        studentMapper.updateEntity(student, request);
        Student updated = studentRepository.save(student);
        log.info("Updated student with id={}", updated.getId());
        return studentMapper.toResponseDTO(updated);
    }

    /**
     * Removes the student from every course first, so no mapping row points at a deleted student.
     * SQLite does not enforce foreign keys by default, so the application must do this itself.
     */
    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudentOrThrow(id);
        courseRepository.findByStudentsId(id)
                .forEach(course -> course.getStudents().removeIf(s -> s.getId().equals(id)));
        studentRepository.delete(student);
        log.info("Deleted student with id={}", id);
    }

    /**
     * Links a student to a user, or removes the link when userId is null.
     * An employee who owns the record can then read it through GET /api/v1/students/{id}.
     */
    @Transactional
    public StudentResponseDTO assignOwner(Long studentId, Long userId) {
        Student student = findStudentOrThrow(studentId);
        if (userId == null) {
            student.setOwner(null);
        } else {
            AppUser owner = appUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            student.setOwner(owner);
        }
        Student updated = studentRepository.save(student);
        log.info("Student id={} owner set to user id={}", studentId, userId);
        return studentMapper.toResponseDTO(updated);
    }

    /**
     * findById returns Optional<Student>, which means "a student or nothing".
     * orElseThrow turns "nothing" into an exception, so no method below ever receives null.
     */
    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }
}
