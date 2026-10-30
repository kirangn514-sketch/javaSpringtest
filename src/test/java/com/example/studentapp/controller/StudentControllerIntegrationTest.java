package com.example.studentapp.controller;

import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Role;
import com.example.studentapp.entity.Student;
import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.StudentRepository;
import com.example.studentapp.support.TestUsers;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the student endpoints, using an ADMIN login.
 * Role-by-role access rules are tested separately in RbacIntegrationTest.
 *
 * @SpringBootTest starts the full application context.
 * @AutoConfigureMockMvc gives us MockMvc, which sends requests straight into the DispatcherServlet.
 * @ActiveProfiles("test") loads application-test.properties, so the database is target/test-student.db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/students";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Login cookie for an admin user. It is created fresh before each test. */
    private Cookie adminCookie;

    /** Start every test with empty tables and a fresh admin login, so tests don't depend on each other. */
    @BeforeEach
    void setUp() throws Exception {
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        adminCookie = new TestUsers(mockMvc, appUserRepository, passwordEncoder)
                .loginAs("student-admin", Role.ADMIN);
    }

    @Test
    void createStudent_returns201_withLocationAndPersistsRow() throws Exception {
        String body = """
                {"name":"Kiran","subject":"Java","address":"India","age":28}
                """;

        mockMvc.perform(post(BASE_URL).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Kiran"));

        assertEquals(1, studentRepository.count());
    }

    @Test
    void createStudent_withInvalidAge_returns400AndPersistsNothing() throws Exception {
        String body = """
                {"name":"Kiran","subject":"Java","address":"India","age":200}
                """;

        mockMvc.perform(post(BASE_URL).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(0, studentRepository.count());
    }

    @Test
    void getStudentById_whenMissing_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999").cookie(adminCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void updateThenDelete_changesAndRemovesRow() throws Exception {
        Student saved = studentRepository.save(newStudent("Kiran", "Java"));
        String body = """
                {"name":"Kiran","subject":"Spring Boot","address":"India","age":28}
                """;

        mockMvc.perform(put(BASE_URL + "/" + saved.getId()).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Spring Boot"));

        mockMvc.perform(delete(BASE_URL + "/" + saved.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        assertEquals(0, studentRepository.count());
    }

    @Test
    void assignOwner_linksStudentToUser() throws Exception {
        Student student = studentRepository.save(newStudent("Kiran", "Java"));
        AppUser employee = new TestUsers(mockMvc, appUserRepository, passwordEncoder)
                .createUser("owner-user", Role.EMPLOYEE);

        mockMvc.perform(put(BASE_URL + "/" + student.getId() + "/owner").cookie(adminCookie)
                        .contentType(APPLICATION_JSON).content("{\"userId\":" + employee.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerUserId").value(employee.getId()));
    }

    @Test
    void assignOwner_withUnknownUser_returns404() throws Exception {
        Student student = studentRepository.save(newStudent("Kiran", "Java"));

        mockMvc.perform(put(BASE_URL + "/" + student.getId() + "/owner").cookie(adminCookie)
                        .contentType(APPLICATION_JSON).content("{\"userId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void getAllStudents_withoutCookie_returns401AndNoData() throws Exception {
        studentRepository.save(newStudent("Kiran", "Java"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getAllStudents_withTamperedCookie_returns401() throws Exception {
        // A token with a broken signature must be rejected, even though it looks like a JWT
        Cookie forged = new Cookie("access_token", "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalid");

        mockMvc.perform(get(BASE_URL).cookie(forged))
                .andExpect(status().isUnauthorized());
    }

    private Student newStudent(String name, String subject) {
        Student student = new Student();
        student.setName(name);
        student.setSubject(subject);
        student.setAddress("India");
        student.setAge(28);
        return student;
    }
}
