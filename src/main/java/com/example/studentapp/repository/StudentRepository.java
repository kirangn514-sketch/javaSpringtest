package com.example.studentapp.repository;

import com.example.studentapp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data-access interface for Student rows.
 *
 * JpaRepository<Student, Long> means:
 *   - Student: the entity this repository manages
 *   - Long: the type of its primary key (the id field)
 *
 * You write no implementation class. Spring Data creates one at startup and
 * registers it as a bean, which the Service then receives through its constructor.
 *
 * It provides these methods without any SQL from you:
 *   save(entity)        INSERT when id is null, UPDATE otherwise
 *   findById(id)        returns Optional<Student>
 *   findAll()           SELECT * FROM students
 *   existsById(id)      true or false, without loading the row
 *   deleteById(id)      DELETE by primary key
 *
 * findByOwnerId is a custom finder. Spring builds the SQL from the method name.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /** All students whose owner is the user with this id. Used when a user is deleted. */
    List<Student> findByOwnerId(Long ownerId);
}
