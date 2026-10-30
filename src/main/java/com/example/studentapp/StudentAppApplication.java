package com.example.studentapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Student Management application.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   - @Configuration: this class can define beans.
 *   - @EnableAutoConfiguration: Spring Boot configures web, JPA, and JDBC from the jars on the classpath.
 *   - @ComponentScan: Spring scans this package and sub-packages (controller, service, ...)
 *     and creates beans for classes annotated with @Service, @Repository, @RestController, etc.
 *
 * Keep this class in the root package (com.example.studentapp). If it moves into a sub-package,
 * the component scan will miss the other packages.
 */
@SpringBootApplication
public class StudentAppApplication {

    public static void main(String[] args) {
        // Starts the embedded Tomcat server and the Spring context
        SpringApplication.run(StudentAppApplication.class, args);
    }
}
