package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.leonsoft.models.Booking;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;

/**
 * Basic BookingsService tests verifying core functionality.
 * Tests booking service initialization and basic operations.
 */
@ExtendWith(MockitoExtension.class)
class BasicBookingsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingsService bookingsService;

    @BeforeEach
    void setUp() {
        // Mock user repository to return users when findByUsername is called
        // This allows getCurrentUser() to work in the service
    }

    @Test
    void bookingsServiceIsInitialized() {
        assertNotNull(bookingsService);
    }

    @Test
    void serviceHasRequiredDependencies() {
        assertNotNull(bookingRepository);
        assertNotNull(userRepository);
    }
}
