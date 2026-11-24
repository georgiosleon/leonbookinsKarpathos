package com.leonsoft.api;

import com.leonsoft.models.Role;
import com.leonsoft.models.User;
import com.leonsoft.repositories.UserRepository;
import com.leonsoft.services.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserRestController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * GET /api/v1/users - List all users (admin only, enforced by SecurityConfig).
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/users/me - The currently authenticated user (any role).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/users - Create new user (admin only).
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        String username = request.username().trim();
        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body("Username must not be blank.");
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                    .body("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(trimToNull(request.email()));
        user.setRole(request.role() == null ? Role.OPERATOR.name() : request.role());
        user.setActive(request.active() == null || request.active());
        User savedUser = userRepository.save(user);
        auditService.record("CREATE", "USER", String.valueOf(savedUser.getId()), "username=" + savedUser.getUsername() + ",role=" + savedUser.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    /**
     * GET /api/v1/users/{id} - Get user by ID (admin only).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/v1/users/{id} - Update user (admin only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        return userRepository.findById(id).map(existingUser -> {
            String username = request.username().trim();
            if (username.isEmpty()) {
                return ResponseEntity.badRequest().body("Username must not be blank.");
            }
            if (!username.equals(existingUser.getUsername()) && userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body("Username already exists: " + username);
            }

            boolean removingAdmin = existingUser.isAdmin()
                    && (!Role.ADMIN.name().equals(request.role()) || Boolean.FALSE.equals(request.active()));
            if (removingAdmin && userRepository.countByRoleAndActiveTrue(Role.ADMIN.name()) <= 1) {
                return ResponseEntity.badRequest().body("Cannot remove or deactivate the last active admin.");
            }

            boolean removingOperator = Role.OPERATOR.name().equals(existingUser.getRole()) && existingUser.isActive()
                    && (!Role.OPERATOR.name().equals(request.role()) || Boolean.FALSE.equals(request.active()));
            if (removingOperator && userRepository.countByRoleAndActiveTrue(Role.OPERATOR.name()) <= 1) {
                return ResponseEntity.badRequest().body("Cannot remove or deactivate the last active operator account.");
            }

            existingUser.setUsername(username);
            existingUser.setEmail(trimToNull(request.email()));
            existingUser.setRole(request.role());
            existingUser.setActive(request.active() == null || request.active());
            if (request.password() != null && !request.password().isBlank()) {
                existingUser.setPasswordHash(passwordEncoder.encode(request.password()));
            }
            User updatedUser = userRepository.save(existingUser);
            auditService.record("UPDATE", "USER", String.valueOf(updatedUser.getId()), "username=" + updatedUser.getUsername() + ",role=" + updatedUser.getRole());
            return ResponseEntity.ok(UserResponse.from(updatedUser));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/users/{id} - Delete user (admin only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id, Authentication authentication) {
        return userRepository.findById(id).map(user -> {
            if (authentication != null && user.getUsername().equals(authentication.getName())) {
                return ResponseEntity.badRequest().body("You cannot delete your own account.");
            }
            if (user.isAdmin() && user.isActive()
                    && userRepository.countByRoleAndActiveTrue(Role.ADMIN.name()) <= 1) {
                return ResponseEntity.badRequest().body("Cannot delete the last active admin.");
            }
            if (Role.OPERATOR.name().equals(user.getRole()) && user.isActive()
                    && userRepository.countByRoleAndActiveTrue(Role.OPERATOR.name()) <= 1) {
                return ResponseEntity.badRequest().body("Cannot delete the last active operator account.");
            }
            userRepository.delete(user);
            auditService.record("DELETE", "USER", String.valueOf(user.getId()), "username=" + user.getUsername() + ",role=" + user.getRole());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
