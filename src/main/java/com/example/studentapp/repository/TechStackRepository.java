package com.example.studentapp.repository;

import com.example.studentapp.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for tech stacks.
 */
@Repository
public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
