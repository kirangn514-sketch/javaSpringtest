package com.example.studentapp.config;

import com.example.studentapp.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;

/**
 * Security rules for the whole application: who may log in, and which roles may call which URL.
 *
 * Request flow for a protected endpoint:
 *   1. The browser sends the access_token cookie with the request.
 *   2. The bearer token resolver reads the token from that cookie (not from a header).
 *   3. The JwtDecoder checks the RS256 signature and the expiry time.
 *   4. The converter reads the "role" claim and grants ROLE_ADMIN, ROLE_HR, or ROLE_EMPLOYEE.
 *   5. The URL rules below decide whether that role may proceed. If not, the request gets 403.
 *   6. Method rules (@PreAuthorize) add finer checks, such as "only the owner of this student".
 *
 * Role access summary:
 *   ADMIN    → everything
 *   HR       → GET /students, GET /students/{id}
 *   EMPLOYEE → GET /students/{id}, only for students they own (checked in StudentController)
 *
 * @EnableWebSecurity turns on Spring Security. @EnableMethodSecurity turns on @PreAuthorize.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Name of the cookie that holds the JWT. */
    public static final String AUTH_COOKIE_NAME = "access_token";

    /**
     * BCrypt is a slow, salted hash designed for passwords. It is the organization's required choice.
     * Never use MD5 or SHA-1 for passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a new 2048-bit RSA key pair each time the app starts.
     * Keys are never written to a file or to application.properties, so no secret is stored in the repo.
     * The cost: restarting the app invalidates every existing login.
     * Production should load a fixed key from Azure Key Vault.
     */
    @Bean
    public KeyPair jwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /** Signs tokens with the private key (RS256). */
    @Bean
    public JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
                .privateKey((RSAPrivateKey) jwtKeyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    /** Checks token signatures with the public key. It holds no private key. */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair jwtKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic()).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // CSRF protection is off because the cookie is SameSite=Strict, so browsers
                // don't send it on cross-site requests. This is a deliberate trade-off.
                .csrf(csrf -> csrf.disable())
                // Stateless: no HTTP session. Every request must carry its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Order matters: the first rule that matches wins, so specific rules come first.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout").permitAll()
                        // Admin-only areas
                        .requestMatchers("/api/v1/users/**", "/api/v1/courses/**", "/api/v1/tech-stacks/**")
                                .hasRole("ADMIN")
                        // HR and admin can list all students
                        .requestMatchers(HttpMethod.GET, "/api/v1/students").hasAnyRole("ADMIN", "HR")
                        // Reading one student is open to all three roles. StudentController checks ownership for employees.
                        .requestMatchers(HttpMethod.GET, "/api/v1/students/*").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                        // Create, update, delete, and owner assignment on students are admin-only
                        .requestMatchers("/api/v1/students/**").hasRole("ADMIN")
                        // Everything else (for example /auth/me) needs any valid token
                        .anyRequest().authenticated())
                // Logged in but wrong role: 403 with the standard wrapper
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDenied) ->
                        writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                "You do not have permission for this action", "FORBIDDEN")))
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Read the role from the token instead of the default "scope" claim
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter()))
                        .bearerTokenResolver(this::resolveTokenFromCookie)
                        // No valid token: 401 with the standard wrapper
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentication required", "UNAUTHORIZED")));
        return http.build();
    }

    /**
     * Turns the token's "role" claim into a Spring authority.
     * The value "ADMIN" becomes the authority "ROLE_ADMIN", which is what hasRole("ADMIN") checks.
     */
    private JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Returns the token from the access_token cookie, or null if the cookie is missing. */
    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> AUTH_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Writes the standard error wrapper, so 401 and 403 responses match every other error. */
    private void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                            int status, String message, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message, errorCode));
    }
}
