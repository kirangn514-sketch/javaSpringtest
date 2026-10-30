package com.example.studentapp.controller;

import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Role;
import com.example.studentapp.entity.Student;
import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.CourseRepository;
import com.example.studentapp.repository.StudentRepository;
import com.example.studentapp.repository.TechStackRepository;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role matrix tests: each role is tried against each kind of endpoint.
 *
 *   ADMIN:    everything allowed
 *   HR:       GET /students and GET /students/{id} allowed; everything else 403
 *   EMPLOYEE: GET /students/{id} allowed only for their own students; everything else 403
 *
 * A 403 response has errorCode FORBIDDEN. A missing or bad cookie gives 401 instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    private static final String STUDENTS = "/api/v1/students";
    private static final String USERS = "/api/v1/users";
    private static final String COURSES = "/api/v1/courses";
    private static final String TECH_STACKS = "/api/v1/tech-stacks";
    private static final String STUDENT_BODY = """
            {"name":"New","subject":"Java","address":"India","age":25}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminCookie;
    private Cookie hrCookie;
    private Cookie employeeCookie;
    private Student ownedByEmployee;
    private Student ownedByOther;

    @BeforeEach
    void setUp() throws Exception {
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        techStackRepository.deleteAll();
        appUserRepository.deleteAll();

        TestUsers users = new TestUsers(mockMvc, appUserRepository, passwordEncoder);
        adminCookie = users.loginAs("rbac-admin", Role.ADMIN);
        hrCookie = users.loginAs("rbac-hr", Role.HR);
        employeeCookie = users.loginAs("rbac-employee", Role.EMPLOYEE);
        AppUser other = users.createUser("rbac-other", Role.EMPLOYEE);
        AppUser employee = appUserRepository.findByUsername("rbac-employee").orElseThrow();

        ownedByEmployee = studentRepository.save(studentOwnedBy(employee, "Mine"));
        ownedByOther = studentRepository.save(studentOwnedBy(other, "Theirs"));
    }

    // ---- ADMIN: full access -------------------------------------------------

    @Test
    void admin_canListStudents() throws Exception {
        mockMvc.perform(get(STUDENTS).cookie(adminCookie)).andExpect(status().isOk());
    }

    @Test
    void admin_canListUsers() throws Exception {
        mockMvc.perform(get(USERS).cookie(adminCookie)).andExpect(status().isOk());
    }

    @Test
    void admin_canListCourses() throws Exception {
        mockMvc.perform(get(COURSES).cookie(adminCookie)).andExpect(status().isOk());
    }

    @Test
    void admin_canCreateStudent() throws Exception {
        mockMvc.perform(post(STUDENTS).cookie(adminCookie).contentType(APPLICATION_JSON).content(STUDENT_BODY))
                .andExpect(status().isCreated());
    }

    // ---- HR: list all students and read one student --------------------------

    @Test
    void hr_canListStudents() throws Exception {
        mockMvc.perform(get(STUDENTS).cookie(hrCookie)).andExpect(status().isOk());
    }

    @Test
    void hr_canReadAnyStudentById() throws Exception {
        mockMvc.perform(get(STUDENTS + "/" + ownedByOther.getId()).cookie(hrCookie))
                .andExpect(status().isOk());
    }

    @Test
    void hr_cannotCreateStudent() throws Exception {
        mockMvc.perform(post(STUDENTS).cookie(hrCookie).contentType(APPLICATION_JSON).content(STUDENT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void hr_cannotDeleteStudent() throws Exception {
        mockMvc.perform(delete(STUDENTS + "/" + ownedByOther.getId()).cookie(hrCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void hr_cannotListUsers() throws Exception {
        mockMvc.perform(get(USERS).cookie(hrCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void hr_cannotAccessCourses() throws Exception {
        mockMvc.perform(get(COURSES).cookie(hrCookie)).andExpect(status().isForbidden());
    }

    // ---- EMPLOYEE: only their own students, by id ----------------------------

    @Test
    void employee_cannotListStudents() throws Exception {
        mockMvc.perform(get(STUDENTS).cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void employee_canReadOwnStudentById() throws Exception {
        mockMvc.perform(get(STUDENTS + "/" + ownedByEmployee.getId()).cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mine"));
    }

    @Test
    void employee_cannotReadAnotherUsersStudent() throws Exception {
        mockMvc.perform(get(STUDENTS + "/" + ownedByOther.getId()).cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void employee_missingStudent_returns403_notA404() throws Exception {
        // An employee gets 403 for a missing ID too, so they can't use the response to find out which IDs exist
        mockMvc.perform(get(STUDENTS + "/999999").cookie(employeeCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotCreateStudent() throws Exception {
        mockMvc.perform(post(STUDENTS).cookie(employeeCookie).contentType(APPLICATION_JSON).content(STUDENT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotListUsers() throws Exception {
        mockMvc.perform(get(USERS).cookie(employeeCookie)).andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotAccessTechStacks() throws Exception {
        mockMvc.perform(get(TECH_STACKS).cookie(employeeCookie)).andExpect(status().isForbidden());
    }

    @Test
    void employee_canStillCallMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"));
    }

    @Test
    void anonymous_cannotListUsers() throws Exception {
        mockMvc.perform(get(USERS)).andExpect(status().isUnauthorized());
    }

    private Student studentOwnedBy(AppUser owner, String name) {
        Student student = new Student();
        student.setName(name);
        student.setSubject("Java");
        student.setAddress("India");
        student.setAge(30);
        student.setOwner(owner);
        return student;
    }
}
