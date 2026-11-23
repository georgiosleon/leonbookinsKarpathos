package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.leonsoft.models.Role;
import com.leonsoft.models.User;
import com.leonsoft.repositories.UserRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for role-based user management: canonical role storage, the
 * authorities that Spring Security matches against the {@code hasRole(...)}
 * expressions, and the login {@link UserDetails} built by
 * {@link SecurityConfig#userDetailsService()}.
 */
class UserManagementTest {

    private User userWithRole(String role) {
        User user = new User();
        user.setUsername("user-" + role);
        user.setRole(role);
        user.setPasswordHash("password_hash");
        user.setEmail("user@test.com");
        user.setActive(true);
        return user;
    }

    private Collection<String> authorities(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @Test
    void roleFromIsCaseInsensitiveAndDefaultsToUser() {
        assertEquals(Role.ADMIN, Role.from("admin"));
        assertEquals(Role.OPERATOR, Role.from("USER"));
        assertEquals(Role.GUEST, Role.from("Guest"));
        assertEquals(Role.OPERATOR, Role.from("unknown"));
        assertEquals(Role.OPERATOR, Role.from(null));
        assertEquals(Role.OPERATOR, Role.from(""));
    }

    @Test
    void getRoleReturnsCanonicalUppercaseName() {
        assertEquals("ADMIN", userWithRole("admin").getRole());
        assertEquals("OPERATOR", userWithRole("USER").getRole());
        assertEquals("GUEST", userWithRole("guest").getRole());
    }

    @Test
    void setRoleIsCaseInsensitive() {
        User user = userWithRole("admin");
        user.setRole("aDmIn");
        assertEquals("ADMIN", user.getRole());
    }

    @Test
    void authoritiesMatchSpringSecurityRoleExpressions() {
        assertTrue(authorities(userWithRole("admin")).contains("ROLE_ADMIN"));
        assertTrue(authorities(userWithRole("USER")).contains("ROLE_OPERATOR"));
        assertTrue(authorities(userWithRole("guest")).contains("ROLE_GUEST"));
    }

    @Test
    void roleHelpersRecognizeRoles() {
        assertTrue(userWithRole("admin").isAdmin());
        assertFalse(userWithRole("admin").isGuest());
        assertTrue(userWithRole("guest").isGuest());
        assertFalse(userWithRole("user").isAdmin());
        assertFalse(new User().isAdmin());
    }

    @Test
    void userDetailsCarryModelAuthoritiesAndActiveFlag() {
        UserRepository userRepository = mock(UserRepository.class);
        SecurityConfig config = new SecurityConfig(userRepository);

        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("admin");
        admin.setPasswordHash("password_hash");
        admin.setEmail("admin@test.com");
        admin.setActive(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        UserDetails adminDetails = config.userDetailsService().loadUserByUsername("admin");
        assertEquals("admin", adminDetails.getUsername());
        assertTrue(adminDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
        assertTrue(adminDetails.isEnabled());

        User guest = new User();
        guest.setUsername("guest");
        guest.setRole("guest");
        guest.setActive(false);
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(guest));
        assertFalse(config.userDetailsService().loadUserByUsername("guest").isEnabled());
    }

    @Test
    void securityUserDetailsForUnknownUserThrows() {
        UserRepository userRepository = mock(UserRepository.class);
        SecurityConfig config = new SecurityConfig(userRepository);
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> config.userDetailsService().loadUserByUsername("nobody"));
    }
}
