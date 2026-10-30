package com.example.studentapp.service;

import com.example.studentapp.dto.StudentRequestDTO;
import com.example.studentapp.dto.StudentResponseDTO;
import com.example.studentapp.entity.Student;
import com.example.studentapp.exception.StudentNotFoundException;
import com.example.studentapp.mapper.StudentMapper;
import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for StudentService. No Spring, no database, no HTTP.
 *
 * The Repository is a Mockito mock: a fake object that returns whatever the test tells it to.
 * That lets each test set up one situation (found, not found, saved) and check the Service's reaction.
 *
 * The Mapper is the real class, not a mock. It is simple, has no dependencies, and mocking it
 * would test nothing useful.
 *
 * Every test follows AAA: Arrange (set up), Act (call the method), Assert (check the result).
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CourseRepository courseRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        // Constructor injection pays off here: we pass the mocks straight in. No Spring needed.
        studentService = new StudentService(studentRepository, new StudentMapper(), appUserRepository, courseRepository);
    }

    @Test
    void createStudent_savesEntityAndReturnsResponseWithGeneratedId() {
        // Arrange
        StudentRequestDTO request = new StudentRequestDTO("Kiran", "Java", "India", 28);
        // Pretend the database assigned id 1. Student has no setId(), so reflection sets the private field.
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // Act
        StudentResponseDTO response = studentService.createStudent(request);

        // Assert
        assertEquals(1L, response.id());
        assertEquals("Kiran", response.name());
        assertEquals("Java", response.subject());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void getStudentById_whenMissing_throwsNotFound() {
        // Arrange
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(StudentNotFoundException.class, () -> studentService.getStudentById(99L));
    }

    @Test
    void getStudentById_whenPresent_returnsResponse() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentWithId(1L, "Kiran")));

        // Act
        StudentResponseDTO response = studentService.getStudentById(1L);

        // Assert
        assertEquals(1L, response.id());
        assertEquals("Kiran", response.name());
    }

    @Test
    void updateStudent_whenPresent_copiesNewValuesAndSaves() {
        // Arrange
        Student existing = studentWithId(1L, "Kiran");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StudentRequestDTO request = new StudentRequestDTO("Kiran", "Spring Boot", "India", 28);

        // Act
        StudentResponseDTO response = studentService.updateStudent(1L, request);

        // Assert
        assertEquals("Spring Boot", response.subject());
        assertEquals(1L, response.id());
        verify(studentRepository).save(existing);
    }

    @Test
    void updateStudent_whenMissing_throwsAndNeverSaves() {
        // Arrange
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        StudentRequestDTO request = new StudentRequestDTO("X", "Y", "Z", 1);

        // Act + Assert: no new row is created for a missing id
        assertThrows(StudentNotFoundException.class, () -> studentService.updateStudent(99L, request));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void deleteStudent_whenPresent_deletesEntity() {
        // Arrange
        Student existing = studentWithId(1L, "Kiran");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        studentService.deleteStudent(1L);

        // Assert
        verify(studentRepository).delete(existing);
    }

    @Test
    void deleteStudent_whenMissing_throwsAndNeverDeletes() {
        // Arrange
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(StudentNotFoundException.class, () -> studentService.deleteStudent(99L));
        verify(studentRepository, never()).delete(any(Student.class));
    }

    /** Builds an entity with an id, the way the database would return it. */
    private Student studentWithId(Long id, String name) {
        Student student = new Student();
        student.setName(name);
        student.setSubject("Java");
        student.setAddress("India");
        student.setAge(28);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
