package com.example.studentapp.controller;

import com.example.studentapp.config.JwtProperties;
import com.example.studentapp.config.SecurityConfig;
import com.example.studentapp.dto.ApiResponse;
import com.example.studentapp.dto.AuthRequestDTO;
import com.example.studentapp.dto.UserResponseDTO;
import com.example.studentapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication endpoints: register, login, logout, and "who am I".
 *
 * The token is never returned in the JSON body. Login puts it in an HttpOnly cookie, so
 * JavaScript on the page can't read it. This protects against token theft through XSS.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    /** POST /api/v1/auth/register creates a user. Returns 201, with no token. Log in next. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody AuthRequestDTO request) {
        UserResponseDTO user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }

    /**
     * POST /api/v1/auth/login checks the credentials and sets the access_token cookie.
     * Set-Cookie is a response header. The browser stores the cookie and sends it on later requests.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody AuthRequestDTO request) {
        String token = authService.login(request);
        ResponseCookie cookie = buildCookie(token, Duration.ofMinutes(jwtProperties.expiryMinutes()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.<Void>success(null, "Login successful"));
    }

    /**
     * POST /api/v1/auth/logout sends an empty cookie with Max-Age=0, which tells the browser to delete it.
     * Tokens are stateless, so a copied token still works until it expires. See the notes below.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = buildCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * GET /api/v1/auth/me returns the logged-in user. Spring fills in Authentication from the token,
     * and its name is the "sub" claim, which is the username.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> me(Authentication authentication) {
        UserResponseDTO user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(user, "Current user retrieved successfully"));
    }

    /**
     * Cookie settings:
     *   HttpOnly: JavaScript can't read it
     *   Secure: sent only over HTTPS (configurable for local HTTP testing)
     *   SameSite=Strict: not sent on requests started from other websites (CSRF protection)
     *   Path=/: sent to every endpoint
     */
    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
