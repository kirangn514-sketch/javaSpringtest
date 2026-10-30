package com.example.studentapp.support;

import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Role;
import com.example.studentapp.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test helper that creates users with a chosen role and logs them in through the real login endpoint.
 * Each test class builds one of these in its setup, passing in the Spring beans it has autowired.
 * It is not a Spring bean itself, so component scanning ignores it.
 */
public class TestUsers {

    private static final String PASSWORD = "password123";

    private final MockMvc mockMvc;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public TestUsers(MockMvc mockMvc, AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates a user with the role, or replaces an existing user with that name. Does not log in. */
    public AppUser createUser(String username, Role role) {
        appUserRepository.findByUsername(username).ifPresent(appUserRepository::delete);
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return appUserRepository.save(user);
    }

    /** Creates the user, logs in through /api/v1/auth/login, and returns the access_token cookie. */
    public Cookie loginAs(String username, Role role) throws Exception {
        createUser(username, role);
        return login(username);
    }

    /** Logs in an existing user with the shared test password and returns the cookie from Set-Cookie. */
    public Cookie login(String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = setCookie.substring("access_token=".length(), setCookie.indexOf(';'));
        return new Cookie("access_token", token);
    }
}
