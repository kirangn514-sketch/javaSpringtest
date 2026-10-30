package com.example.studentapp.service;

import com.example.studentapp.dto.CreateUserRequestDTO;
import com.example.studentapp.dto.UpdateUserRequestDTO;
import com.example.studentapp.dto.UserResponseDTO;
import com.example.studentapp.entity.AppUser;
import com.example.studentapp.exception.UserNotFoundException;
import com.example.studentapp.exception.UsernameAlreadyExistsException;
import com.example.studentapp.mapper.UserMapper;
import com.example.studentapp.repository.AppUserRepository;
import com.example.studentapp.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management for admins: create, list, read, update, and delete users, including their roles.
 * Every method here is restricted to ADMIN by the security rules in SecurityConfig.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(AppUserRepository appUserRepository,
                       StudentRepository studentRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        return userMapper.toResponseDTO(findUserOrThrow(id));
    }

    @Transactional
    public UserResponseDTO createUser(CreateUserRequestDTO request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        AppUser saved = appUserRepository.save(user);
        log.info("Admin created user id={} with role {}", saved.getId(), saved.getRole());
        return userMapper.toResponseDTO(saved);
    }

    /**
     * Updates the username, role, and (optionally) the password.
     * A null password keeps the current one. The role change applies on the user's next login.
     */
    @Transactional
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request) {
        AppUser user = findUserOrThrow(id);
        if (appUserRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new UsernameAlreadyExistsException();
        }
        user.setUsername(request.username());
        user.setRole(request.role());
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        AppUser updated = appUserRepository.save(user);
        log.info("Admin updated user id={} with role {}", updated.getId(), updated.getRole());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Deletes the user. Students owned by this user are unlinked first, so they are kept but have no owner.
     */
    @Transactional
    public void deleteUser(Long id) {
        AppUser user = findUserOrThrow(id);
        studentRepository.findByOwnerId(id).forEach(student -> student.setOwner(null));
        appUserRepository.delete(user);
        log.info("Admin deleted user id={}", id);
    }

    private AppUser findUserOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
