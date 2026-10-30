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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for user management (ADMIN only): create, list, read, update, delete, and role changes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    private static final String USERS = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestUsers users;
    private Cookie adminCookie;

    @BeforeEach
    void setUp() throws Exception {
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        users = new TestUsers(mockMvc, appUserRepository, passwordEncoder);
        adminCookie = users.loginAs("user-admin", Role.ADMIN);
    }

    @Test
    void createUser_withRole_returns201_andNoPassword() throws Exception {
        String body = """
                {"username":"new-hire","password":"password123","role":"HR"}
                """;

        mockMvc.perform(post(USERS).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("new-hire"))
                .andExpect(jsonPath("$.data.role").value("HR"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void createUser_withTakenUsername_returns409() throws Exception {
        users.createUser("taken", Role.EMPLOYEE);
        String body = """
                {"username":"taken","password":"password123","role":"HR"}
                """;

        mockMvc.perform(post(USERS).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }

    @Test
    void createUser_withoutRole_returns400() throws Exception {
        String body = """
                {"username":"no-role","password":"password123"}
                """;

        mockMvc.perform(post(USERS).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void listUsers_returnsAll() throws Exception {
        users.createUser("someone", Role.EMPLOYEE);

        mockMvc.perform(get(USERS).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updateUser_changesRole() throws Exception {
        AppUser employee = users.createUser("promote-me", Role.EMPLOYEE);
        String body = """
                {"username":"promote-me","role":"HR"}
                """;

        mockMvc.perform(put(USERS + "/" + employee.getId()).cookie(adminCookie)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("HR"));
    }

    @Test
    void updateUser_withoutPassword_keepsOldPassword() throws Exception {
        AppUser employee = users.createUser("keep-pass", Role.EMPLOYEE);
        String body = """
                {"username":"keep-pass","role":"EMPLOYEE"}
                """;

        mockMvc.perform(put(USERS + "/" + employee.getId()).cookie(adminCookie)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        // The original password still works
        users.login("keep-pass");
    }

    @Test
    void updateUser_toTakenUsername_returns409() throws Exception {
        users.createUser("first", Role.EMPLOYEE);
        AppUser second = users.createUser("second", Role.EMPLOYEE);
        String body = """
                {"username":"first","role":"EMPLOYEE"}
                """;

        mockMvc.perform(put(USERS + "/" + second.getId()).cookie(adminCookie)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }

    @Test
    void deleteUser_keepsTheirStudentsButUnlinksThem() throws Exception {
        AppUser employee = users.createUser("leaving", Role.EMPLOYEE);
        Student student = new Student();
        student.setName("Kept");
        student.setSubject("Java");
        student.setAddress("India");
        student.setAge(30);
        student.setOwner(employee);
        Student saved = studentRepository.save(student);

        mockMvc.perform(delete(USERS + "/" + employee.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        Student reloaded = studentRepository.findById(saved.getId()).orElseThrow();
        assertNull(reloaded.getOwner());
    }

    @Test
    void getMissingUser_returns404() throws Exception {
        mockMvc.perform(get(USERS + "/999999").cookie(adminCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }
}
