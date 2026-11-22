package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leonsoft.models.User;
import com.leonsoft.repositories.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the idempotent startup seeding of default accounts by
 * {@link DatabaseInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {

    @InjectMocks
    private DatabaseInitializer initializer;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @BeforeEach
    void setDefaultAccounts() {
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", "adminadminadmin");
        ReflectionTestUtils.setField(initializer, "operatorUsername", "user");
        ReflectionTestUtils.setField(initializer, "operatorPassword", "user12345678");
        ReflectionTestUtils.setField(initializer, "guestUsername", "guest");
        ReflectionTestUtils.setField(initializer, "guestPassword", "guest12345678");
    }

    @Test
    void seedsDefaultAccountsWhenMissing() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("guest")).thenReturn(Optional.empty());

        initializer.init();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());
        Map<String, User> byUsername = captor.getAllValues().stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity()));
        assertEquals("ADMIN", byUsername.get("admin").getRole());
        assertEquals("OPERATOR", byUsername.get("user").getRole());
        assertEquals("GUEST", byUsername.get("guest").getRole());
        assertEquals(true, byUsername.get("admin").isActive());
    }

    @Test
    void doesNotOverwriteExistingUsers() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("guest")).thenReturn(Optional.empty());

        initializer.init();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        assertEquals(Set.of("user", "guest"),
                captor.getAllValues().stream().map(User::getUsername).collect(Collectors.toSet()));
    }

    @Test
    void seedsOnlyTheMissingAccounts() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("guest")).thenReturn(Optional.empty());

        initializer.init();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        assertEquals(Set.of("admin", "guest"),
                captor.getAllValues().stream().map(User::getUsername).collect(Collectors.toSet()));
    }

    @Test
    void accountNamesAndPasswordsAreConfigurable() {
        ReflectionTestUtils.setField(initializer, "adminUsername", "root");
        ReflectionTestUtils.setField(initializer, "adminPassword", "rootpassword12");
        ReflectionTestUtils.setField(initializer, "operatorUsername", "member");
        ReflectionTestUtils.setField(initializer, "operatorPassword", "memberpass12");
        ReflectionTestUtils.setField(initializer, "guestUsername", "visitor");
        ReflectionTestUtils.setField(initializer, "guestPassword", "visitorpass12");
        when(userRepository.findByUsername("root")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("member")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("visitor")).thenReturn(Optional.empty());

        initializer.init();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());
        assertEquals(Set.of("root", "member", "visitor"),
                captor.getAllValues().stream().map(User::getUsername).collect(Collectors.toSet()));
    }
}
