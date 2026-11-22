package com.leonsoft.config;

import com.leonsoft.models.User;
import com.leonsoft.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.password}")
    private String adminPassword;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void init() {
        // Create admin user if it doesn't exist
        if (adminUsername != null && !adminUsername.isBlank()) {
            userRepository.findByUsername(adminUsername)
                    .ifPresentOrElse(
                            existing -> {
                                // Ensure password is encoded correctly
                                if (!existing.getPasswordHash().startsWith("$2a$") && !existing.getPasswordHash().startsWith("$2b$")) {
                                    existing.setPasswordHash(passwordEncoder.encode(adminPassword));
                                    userRepository.save(existing);
                                }
                            },
                            () -> {
                                User admin = new User();
                                admin.setUsername(adminUsername);
                                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                                admin.setRole("admin");
                                admin.setActive(true);
                                userRepository.save(admin);
                            });
        }
    }
}