package com.example.studentapp.service;

import com.example.studentapp.dto.CourseRequestDTO;
import com.example.studentapp.dto.CourseResponseDTO;
import com.example.studentapp.entity.Course;
import com.example.studentapp.entity.Student;
import com.example.studentapp.entity.TechStack;
import com.example.studentapp.exception.CourseNotFoundException;
import com.example.studentapp.exception.DuplicateNameException;
import com.example.studentapp.exception.StudentNotFoundException;
import com.example.studentapp.exception.TechStackNotFoundException;
import com.example.studentapp.mapper.CourseMapper;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.StudentRepository;
import com.example.studentapp.repository.TechStackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for courses and their links to tech stacks and students.
 *
 * Linking and unlinking changes the mapping tables course_tech_stack and course_student.
 * Both sets are Sets, so linking the same item twice does nothing.
 * Restricted to ADMIN by the security rules.
 */
@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final TechStackRepository techStackRepository;
    private final StudentRepository studentRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository,
                         TechStackRepository techStackRepository,
                         StudentRepository studentRepository,
                         CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.techStackRepository = techStackRepository;
        this.studentRepository = studentRepository;
        this.courseMapper = courseMapper;
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getCourseById(Long id) {
        return courseMapper.toResponseDTO(findCourseOrThrow(id));
    }

    @Transactional
    public CourseResponseDTO createCourse(CourseRequestDTO request) {
        if (courseRepository.existsByName(request.name())) {
            throw new DuplicateNameException("Course");
        }
        Course saved = courseRepository.save(courseMapper.toEntity(request));
        log.info("Created course id={}", saved.getId());
        return courseMapper.toResponseDTO(saved);
    }

    @Transactional
    public CourseResponseDTO updateCourse(Long id, CourseRequestDTO request) {
        Course course = findCourseOrThrow(id);
        if (courseRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateNameException("Course");
        }
        courseMapper.updateEntity(course, request);
        return courseMapper.toResponseDTO(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = findCourseOrThrow(id);
        courseRepository.delete(course);
        log.info("Deleted course id={}", id);
    }

    @Transactional
    public CourseResponseDTO addTechStack(Long courseId, Long techStackId) {
        Course course = findCourseOrThrow(courseId);
        TechStack techStack = techStackRepository.findById(techStackId)
                .orElseThrow(() -> new TechStackNotFoundException(techStackId));
        course.getTechStacks().add(techStack);
        return courseMapper.toResponseDTO(course);
    }

    @Transactional
    public CourseResponseDTO removeTechStack(Long courseId, Long techStackId) {
        Course course = findCourseOrThrow(courseId);
        course.getTechStacks().removeIf(t -> t.getId().equals(techStackId));
        return courseMapper.toResponseDTO(course);
    }

    @Transactional
    public CourseResponseDTO addStudent(Long courseId, Long studentId) {
        Course course = findCourseOrThrow(courseId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        course.getStudents().add(student);
        return courseMapper.toResponseDTO(course);
    }

    @Transactional
    public CourseResponseDTO removeStudent(Long courseId, Long studentId) {
        Course course = findCourseOrThrow(courseId);
        course.getStudents().removeIf(s -> s.getId().equals(studentId));
        return courseMapper.toResponseDTO(course);
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }
}
