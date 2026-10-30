package com.example.studentapp.config;

import com.example.studentapp.entity.AppUser;
import com.example.studentapp.entity.Role;
import com.example.studentapp.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin, HR, and employee accounts when the app starts.
 *
 * Passwords are never written in the code or in application.properties. They come from
 * environment variables. If a variable is missing or too short, that account is skipped
 * with a warning. An account that already exists is never changed.
 *
 *   SEED_ADMIN_PASSWORD      → user "admin"    (role ADMIN)
 *   SEED_HR_PASSWORD         → user "hr_user"  (role HR)
 *   SEED_EMPLOYEE_PASSWORD   → user "employee" (role EMPLOYEE)
 *
 * Usernames must be at least 3 characters, so the HR account is "hr_user" rather than "hr".
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String hrPassword;
    private final String employeePassword;

    public DataSeeder(AppUserRepository appUserRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password:}") String adminPassword,
                      @Value("${app.seed.hr-password:}") String hrPassword,
                      @Value("${app.seed.employee-password:}") String employeePassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.hrPassword = hrPassword;
        this.employeePassword = employeePassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", Role.ADMIN, adminPassword);
        seed("hr_user", Role.HR, hrPassword);
        seed("employee", Role.EMPLOYEE, employeePassword);
    }

    private void seed(String username, Role role, String password) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Seed user '{}' not created: set its password environment variable (at least {} characters)",
                    username, MIN_PASSWORD_LENGTH);
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        appUserRepository.save(user);
        log.info("Seed user '{}' created with role {}", username, role);
    }
}
