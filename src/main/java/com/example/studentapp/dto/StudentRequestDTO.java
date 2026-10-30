package com.example.studentapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data that the client sends when creating or updating a student (the JSON request body).
 *
 * This is a DTO (Data Transfer Object): a plain data carrier between the client and the API.
 * It is not a database table and not a service.
 *
 * Bean Validation annotations describe the rules. Spring checks them when the Controller
 * has @Valid on the parameter. A failing check throws MethodArgumentNotValidException,
 * which the GlobalExceptionHandler turns into a 400 response.
 *   @NotBlank: not null, and not empty or only spaces
 *   @NotNull:  not null (allows 0, unlike @NotBlank for strings)
 *   @Size:     string length limits
 *   @Min/@Max: numeric range limits
 *
 * It is a Java record: the compiler generates the constructor, getters (name(), subject(), ...),
 * equals, hashCode, and toString. Records are immutable, so the values can't change after creation.
 * Jackson can convert JSON into a record directly. Annotations on record components are copied to
 * the field, so the validator can check them.
 *
 * "id" is not included on purpose. The database generates it, and the client must not choose it.
 */
public record StudentRequestDTO(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "subject is required")
        @Size(max = 100, message = "subject must be at most 100 characters")
        String subject,

        // Optional: no constraint, so null or empty is allowed
        @Size(max = 255, message = "address must be at most 255 characters")
        String address,

        @NotNull(message = "age is required")
        @Min(value = 1, message = "age must be at least 1")
        @Max(value = 120, message = "age must be at most 120")
        Integer age
) {
}
