package com.example.studentapp.mapper;

import com.example.studentapp.dto.TechStackRequestDTO;
import com.example.studentapp.dto.TechStackResponseDTO;
import com.example.studentapp.entity.TechStack;
import org.springframework.stereotype.Component;

/**
 * Copies data between tech stack DTOs and the TechStack entity.
 */
@Component
public class TechStackMapper {

    public TechStack toEntity(TechStackRequestDTO request) {
        TechStack techStack = new TechStack();
        techStack.setName(request.name());
        return techStack;
    }

    public TechStackResponseDTO toResponseDTO(TechStack techStack) {
        return new TechStackResponseDTO(techStack.getId(), techStack.getName());
    }

    public void updateEntity(TechStack techStack, TechStackRequestDTO request) {
        techStack.setName(request.name());
    }
}
