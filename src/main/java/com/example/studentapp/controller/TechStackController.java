package com.example.studentapp.controller;

import com.example.studentapp.dto.ApiResponse;
import com.example.studentapp.dto.TechStackRequestDTO;
import com.example.studentapp.dto.TechStackResponseDTO;
import com.example.studentapp.service.TechStackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Tech stack endpoints. ADMIN only, enforced by SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/tech-stacks")
public class TechStackController {

    private final TechStackService techStackService;

    public TechStackController(TechStackService techStackService) {
        this.techStackService = techStackService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TechStackResponseDTO>>> getAllTechStacks() {
        return ResponseEntity.ok(ApiResponse.success(techStackService.getAllTechStacks(), "Tech stacks retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TechStackResponseDTO>> getTechStackById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(techStackService.getTechStackById(id), "Tech stack retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TechStackResponseDTO>> createTechStack(@Valid @RequestBody TechStackRequestDTO request) {
        TechStackResponseDTO created = techStackService.createTechStack(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(created, "Tech stack created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TechStackResponseDTO>> updateTechStack(
            @PathVariable Long id,
            @Valid @RequestBody TechStackRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(techStackService.updateTechStack(id, request), "Tech stack updated successfully"));
    }

    /** DELETE /api/v1/tech-stacks/{id} returns 204 and removes the tech stack from every course. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTechStack(@PathVariable Long id) {
        techStackService.deleteTechStack(id);
        return ResponseEntity.noContent().build();
    }
}
