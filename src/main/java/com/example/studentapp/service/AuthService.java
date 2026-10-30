package com.example.studentapp.service;

import com.example.studentapp.config.JwtProperties;
import com.example.studentapp.dto.AuthRequestDTO;
import com.example.studentapp.dto.UserResponseDTO;
import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Role;
import com.example.studentapp.exception.InvalidCredentialsException;
import com.example.studentapp.exception.UsernameAlreadyExistsException;
import com.example.studentapp.mapper.UserMapper;
import com.example.studentapp.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Registration, login, and token creation.
 *
 * This class never sees cookies or HTTP. The controller turns the token into a cookie.
 * The token carries the user's role, so the security rules can check it on each request.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Stored in the token's "iss" claim. Tokens from any other issuer are rejected. */
    private static final String ISSUER = "student-app";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       JwtProperties jwtProperties,
                       UserMapper userMapper) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
    }

    /**
     * Public registration. Every new account is an EMPLOYEE. Only an ADMIN can create other roles,
     * so nobody can make themselves an admin by registering.
     */
    @Transactional
    public UserResponseDTO register(AuthRequestDTO request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        // The plain password is hashed here and then discarded. Only the hash is saved.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.EMPLOYEE);
        AppUser saved = appUserRepository.save(user);
        log.info("Registered user id={}", saved.getId());
        return userMapper.toResponseDTO(saved);
    }

    /**
     * Checks the password against the stored hash and returns a signed token.
     * The same exception is thrown for a wrong username and a wrong password.
     */
    @Transactional(readOnly = true)
    public String login(AuthRequestDTO request) {
        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        log.info("User id={} logged in with role {}", user.getId(), user.getRole());
        return generateToken(user.getUsername(), user.getRole());
    }

    /** Returns the user for a token's subject. Used by GET /me. */
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        return userMapper.toResponseDTO(user);
    }

    /**
     * Builds a JWT with these claims:
     *   iss: who issued it, sub: the username, iat: issue time, exp: expiry time,
     *   role: the user's role (ADMIN, HR, or EMPLOYEE), used for authorization
     * The header says RS256, so the token is signed with the private key.
     *
     * A role change takes effect only when the user logs in again, because the old token keeps the old role.
     */
    private String generateToken(String username, Role role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiryMinutes(), ChronoUnit.MINUTES))
                .claim("role", role.name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
