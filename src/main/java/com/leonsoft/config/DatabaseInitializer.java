package com.leonsoft.config;

import com.leonsoft.models.Role;
import com.leonsoft.models.User;
import com.leonsoft.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default accounts into the users table. Accounts are created only when
 * they are missing; an existing database is never modified on startup, so
 * seeding is idempotent and users stay under the administrator's control.
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final int MIN_PASSWORD_LENGTH = 12;

    @Value("${APP_ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${APP_ADMIN_PASSWORD:change-me-admin-password}")
    private String adminPassword;

    @Value("${APP_OPERATOR_USERNAME:user}")
    private String operatorUsername;

    @Value("${APP_OPERATOR_PASSWORD:change-me-user-password}")
    private String operatorPassword;

    @Value("${APP_GUEST_USERNAME:guest}")
    private String guestUsername;

    @Value("${APP_GUEST_PASSWORD:change-me-guest-password}")
    private String guestPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @PostConstruct
    @Transactional
    public void init() {
        migrateLegacyRoles();
        createUserIfAbsent(adminUsername, adminPassword, Role.ADMIN.name(), "admin@test.com");
        createUserIfAbsent(operatorUsername, operatorPassword, Role.OPERATOR.name(), "operator@test.com");
        createUserIfAbsent(guestUsername, guestPassword, Role.GUEST.name(), "guest@test.com");
    }

    private void migrateLegacyRoles() {
        for (User user : userRepository.findAll()) {
            String stored = user.getStoredRole();
            String canonical = Role.from(stored).name();
            if (stored == null || !canonical.equals(stored)) {
                user.setRole(canonical);
                userRepository.save(user);
                log.info("Migrated legacy role for user '{}' to {}", user.getUsername(), canonical);
            }
        }
    }

    private void createUserIfAbsent(String username, String password, String role, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Seed password for " + role + " account '" + username
                            + "' must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (password.startsWith("change-me-")) {
            boolean prod = environment != null && Arrays.stream(environment.getActiveProfiles())
                    .anyMatch("prod"::equalsIgnoreCase);
            String message = "Using built-in default seed password for " + role + " account '" + username
                    + "'. Set the corresponding APP_*_PASSWORD environment variable before deployment.";
            if (prod) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmail(email);
        user.setActive(true);
        userRepository.save(user);
        log.info("Created default {} account '{}' (email: {})", role, username, email);
    }
}
