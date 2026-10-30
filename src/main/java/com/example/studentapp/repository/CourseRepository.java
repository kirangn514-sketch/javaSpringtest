package com.example.studentapp.repository;

import com.example.studentapp.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for courses.
 *
 * The finders that look through a relationship use a dotted name in the method:
 *   findByStudentsId       → courses that contain the student with this id (uses course_student)
 *   findByTechStacksId     → courses that use the tech stack with this id (uses course_tech_stack)
 * Both are needed so that deleting a student or tech stack can also remove its mapping rows.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Course> findByStudentsId(Long studentId);

    List<Course> findByTechStacksId(Long techStackId);
}
