package com.example.studentapp.controller;

import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.StudentRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for register, login, logout, and /me.
 * These check the cookie flags the browser depends on: HttpOnly, SameSite=Strict, and the expiry.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String CREDENTIALS = """
            {"username":"kiran","password":"password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void clearUsers() {
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void register_returns201_andStoresHashNotPassword() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("kiran"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        var stored = appUserRepository.findByUsername("kiran").orElseThrow();
        assertNotEquals("password123", stored.getPasswordHash());
        assertTrue(stored.getPasswordHash().startsWith("$2"), "BCrypt hashes start with $2");
    }

    @Test
    void register_withTakenUsername_returns409() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS));

        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }

    @Test
    void login_setsHttpOnlyTokenCookie_andKeepsTokenOutOfBody() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS));

        MvcResult result = mockMvc.perform(post(AUTH_URL + "/login").contentType(APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.startsWith("access_token="), "cookie name");
        assertTrue(setCookie.contains("HttpOnly"), "JavaScript must not be able to read it");
        assertTrue(setCookie.contains("SameSite=Strict"), "must not be sent on cross-site requests");
        assertTrue(setCookie.contains("Path=/"), "must be sent to every endpoint");
        assertTrue(setCookie.contains("Max-Age=1800"), "30 minutes, from app.jwt.expiry-minutes");
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS));

        mockMvc.perform(post(AUTH_URL + "/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"kiran","password":"wrongpass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withUnknownUsername_returns401WithSameMessage() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/login").contentType(APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void me_withValidCookie_returnsCurrentUser() throws Exception {
        Cookie cookie = loginAndGetCookie();

        mockMvc.perform(get(AUTH_URL + "/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("kiran"));
    }

    @Test
    void me_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get(AUTH_URL + "/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookieWithMaxAgeZero() throws Exception {
        MvcResult result = mockMvc.perform(post(AUTH_URL + "/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.startsWith("access_token=;"), "value cleared");
        assertTrue(setCookie.contains("Max-Age=0"), "browser deletes the cookie");
    }

    /** Registers the user, logs in, and returns the cookie from the login response. */
    private Cookie loginAndGetCookie() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register").contentType(APPLICATION_JSON).content(CREDENTIALS));
        MvcResult result = mockMvc.perform(post(AUTH_URL + "/login").contentType(APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = setCookie.substring("access_token=".length(), setCookie.indexOf(';'));
        assertEquals(3, token.split("\\.").length, "a JWT has three parts: header.payload.signature");
        return new Cookie("access_token", token);
    }
}
