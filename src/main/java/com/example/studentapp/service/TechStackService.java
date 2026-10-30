package com.example.studentapp.service;

import com.example.studentapp.dto.TechStackRequestDTO;
import com.example.studentapp.dto.TechStackResponseDTO;
import com.example.studentapp.entity.TechStack;
import com.example.studentapp.exception.DuplicateNameException;
import com.example.studentapp.exception.TechStackNotFoundException;
import com.example.studentapp.mapper.TechStackMapper;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.TechStackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for tech stacks. Restricted to ADMIN by the security rules.
 */
@Service
public class TechStackService {

    private static final Logger log = LoggerFactory.getLogger(TechStackService.class);

    private final TechStackRepository techStackRepository;
    private final CourseRepository courseRepository;
    private final TechStackMapper techStackMapper;

    public TechStackService(TechStackRepository techStackRepository,
                            CourseRepository courseRepository,
                            TechStackMapper techStackMapper) {
        this.techStackRepository = techStackRepository;
        this.courseRepository = courseRepository;
        this.techStackMapper = techStackMapper;
    }

    @Transactional(readOnly = true)
    public List<TechStackResponseDTO> getAllTechStacks() {
        return techStackRepository.findAll()
                .stream()
                .map(techStackMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TechStackResponseDTO getTechStackById(Long id) {
        return techStackMapper.toResponseDTO(findOrThrow(id));
    }

    @Transactional
    public TechStackResponseDTO createTechStack(TechStackRequestDTO request) {
        if (techStackRepository.existsByName(request.name())) {
            throw new DuplicateNameException("Tech stack");
        }
        TechStack saved = techStackRepository.save(techStackMapper.toEntity(request));
        log.info("Created tech stack id={}", saved.getId());
        return techStackMapper.toResponseDTO(saved);
    }

    @Transactional
    public TechStackResponseDTO updateTechStack(Long id, TechStackRequestDTO request) {
        TechStack techStack = findOrThrow(id);
        if (techStackRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateNameException("Tech stack");
        }
        techStackMapper.updateEntity(techStack, request);
        return techStackMapper.toResponseDTO(techStackRepository.save(techStack));
    }

    /**
     * Removes the tech stack from every course that uses it, then deletes it.
     * SQLite does not enforce foreign keys by default, so the mapping rows are cleaned up here.
     */
    @Transactional
    public void deleteTechStack(Long id) {
        TechStack techStack = findOrThrow(id);
        courseRepository.findByTechStacksId(id)
                .forEach(course -> course.getTechStacks().removeIf(t -> t.getId().equals(id)));
        techStackRepository.delete(techStack);
        log.info("Deleted tech stack id={}", id);
    }

    private TechStack findOrThrow(Long id) {
        return techStackRepository.findById(id)
                .orElseThrow(() -> new TechStackNotFoundException(id));
    }
}
