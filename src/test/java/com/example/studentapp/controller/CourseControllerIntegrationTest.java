package com.example.studentapp.controller;

import com.example.studentapp.entity.Course;
import com.example.studentapp.entity.Role;
import com.example.studentapp.entity.Student;
import com.example.studentapp.entity.TechStack;
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
 * Tests for courses and their mapping tables (course_tech_stack and course_student). ADMIN only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseControllerIntegrationTest {

    private static final String COURSES = "/api/v1/courses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminCookie;

    @BeforeEach
    void setUp() throws Exception {
        courseRepository.deleteAll();
        techStackRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        adminCookie = new TestUsers(mockMvc, appUserRepository, passwordEncoder)
                .loginAs("course-admin", Role.ADMIN);
    }

    @Test
    void createCourse_returns201() throws Exception {
        String body = """
                {"name":"Backend","description":"Server side development"}
                """;

        mockMvc.perform(post(COURSES).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Backend"))
                .andExpect(jsonPath("$.data.techStacks").isEmpty())
                .andExpect(jsonPath("$.data.studentIds").isEmpty());
    }

    @Test
    void createCourse_withTakenName_returns409() throws Exception {
        saveCourse("Backend");
        String body = """
                {"name":"Backend"}
                """;

        mockMvc.perform(post(COURSES).cookie(adminCookie).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NAME_TAKEN"));
    }

    @Test
    void linkAndUnlinkTechStack() throws Exception {
        Course course = saveCourse("Backend");
        TechStack java = saveTechStack("Java");

        mockMvc.perform(post(COURSES + "/" + course.getId() + "/tech-stacks/" + java.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.techStacks[0].name").value("Java"));

        mockMvc.perform(delete(COURSES + "/" + course.getId() + "/tech-stacks/" + java.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.techStacks").isEmpty());
    }

    @Test
    void linkingSameTechStackTwice_doesNotDuplicate() throws Exception {
        Course course = saveCourse("Backend");
        TechStack java = saveTechStack("Java");
        String url = COURSES + "/" + course.getId() + "/tech-stacks/" + java.getId();

        mockMvc.perform(post(url).cookie(adminCookie)).andExpect(status().isOk());
        mockMvc.perform(post(url).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.techStacks.length()").value(1));
    }

    @Test
    void enrollAndRemoveStudent() throws Exception {
        Course course = saveCourse("Backend");
        Student student = saveStudent("Kiran");

        mockMvc.perform(post(COURSES + "/" + course.getId() + "/students/" + student.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentIds[0]").value(student.getId()));

        mockMvc.perform(delete(COURSES + "/" + course.getId() + "/students/" + student.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentIds").isEmpty());
    }

    @Test
    void deletingStudent_removesTheirEnrollment() throws Exception {
        Course course = saveCourse("Backend");
        Student student = saveStudent("Kiran");
        mockMvc.perform(post(COURSES + "/" + course.getId() + "/students/" + student.getId()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/students/" + student.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(COURSES + "/" + course.getId()).cookie(adminCookie))
                .andExpect(jsonPath("$.data.studentIds").isEmpty());
    }

    @Test
    void deletingTechStack_removesItFromCourses() throws Exception {
        Course course = saveCourse("Backend");
        TechStack java = saveTechStack("Java");
        mockMvc.perform(post(COURSES + "/" + course.getId() + "/tech-stacks/" + java.getId()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/tech-stacks/" + java.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(COURSES + "/" + course.getId()).cookie(adminCookie))
                .andExpect(jsonPath("$.data.techStacks").isEmpty());
    }

    @Test
    void linkUnknownTechStack_returns404() throws Exception {
        Course course = saveCourse("Backend");

        mockMvc.perform(post(COURSES + "/" + course.getId() + "/tech-stacks/999999").cookie(adminCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TECH_STACK_NOT_FOUND"));
    }

    @Test
    void enrollUnknownStudent_returns404() throws Exception {
        Course course = saveCourse("Backend");

        mockMvc.perform(post(COURSES + "/" + course.getId() + "/students/999999").cookie(adminCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void getMissingCourse_returns404() throws Exception {
        mockMvc.perform(get(COURSES + "/999999").cookie(adminCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COURSE_NOT_FOUND"));
    }

    @Test
    void deleteCourse_returns204() throws Exception {
        Course course = saveCourse("Backend");

        mockMvc.perform(delete(COURSES + "/" + course.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(COURSES + "/" + course.getId()).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    private Course saveCourse(String name) {
        Course course = new Course();
        course.setName(name);
        return courseRepository.save(course);
    }

    private TechStack saveTechStack(String name) {
        TechStack techStack = new TechStack();
        techStack.setName(name);
        return techStackRepository.save(techStack);
    }

    private Student saveStudent(String name) {
        Student student = new Student();
        student.setName(name);
        student.setSubject("Java");
        student.setAddress("India");
        student.setAge(28);
        return studentRepository.save(student);
    }
}
