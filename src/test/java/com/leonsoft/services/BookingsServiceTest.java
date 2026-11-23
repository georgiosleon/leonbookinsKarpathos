package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingStatus;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookingsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingResourceService resourceService;

    @Mock
    private UserRepository userRepository;

    private BookingsService bookingsService;

    @BeforeEach
    void setUp() {
        bookingsService = new BookingsService(
              bookingRepository, resourceService, new BookingAvailabilityPolicy(true, true), new BookingOverlapChecker(bookingRepository), new CurrentUserProvider(userRepository));
        User admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setActive(true);
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("admin", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNormalizesDatesAndRecalculatesNightsAndBalance() {
        Booking booking = booking("20/06/2026", "23/06/2026");
        booking.setNumOfNights(99);
        booking.setBalance(new java.math.BigDecimal("999.0"));
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            assertEquals(java.time.LocalDate.parse("2026-06-20"), saved.getStartDate());
            assertEquals(java.time.LocalDate.parse("2026-06-23"), saved.getEndDate());
            assertEquals(3, saved.getNumOfNights());
            assertEquals(new java.math.BigDecimal("70.00"), saved.getBalance());
            return saved;
        });

        Booking result = bookingsService.create(booking);

        assertNotNull(result.getId());
        assertEquals(java.time.LocalDate.parse("2026-06-20"), result.getStartDate());
        assertEquals(java.time.LocalDate.parse("2026-06-23"), result.getEndDate());
        assertEquals(3, result.getNumOfNights());
        assertEquals(new java.math.BigDecimal("70.00"), result.getBalance());
        assertEquals("APT 2", result.getRoomName());
    }

    @Test
    void updateKeepsOriginalRequestTimestamp() {
        User user = new User();
        user.setId(2);
        user.setUsername("user");
        user.setRole("OPERATOR");
        user.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        User regular = new User();
        regular.setId(2);
        regular.setUsername("user");
        regular.setRole("OPERATOR");
        regular.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regular));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("user", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Booking existing = new Booking();
        existing.setRequestDateTime(createdAt);
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking update = booking("20/06/2026", "22/06/2026");
        update.setId("booking-1");
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));

        Booking result = bookingsService.update(update);

        assertEquals(createdAt, result.getRequestDateTime());
        assertEquals("APT 2", result.getRoomName());
        verify(bookingRepository).save(update);
    }

    @Test
    void updateReturnsNotFoundWhenBookingDoesNotExist() {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());
        Booking update = new Booking();
        update.setId("missing");

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.update(update));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void legacyDeleteOnlyRemovesCancelledBookings() {
        User user = new User();
        user.setId(2);
        user.setUsername("user");
        user.setRole("OPERATOR");
        user.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        User regular = new User();
        regular.setId(2);
        regular.setUsername("user");
        regular.setRole("OPERATOR");
        regular.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regular));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("user", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        Booking cancelled = savedBooking("cancelled-1", "2026-06-20", "2026-06-23", "cancelled");
        when(bookingRepository.findById("cancelled-1")).thenReturn(Optional.of(cancelled));

        bookingsService.delCancelledBooking("cancelled-1");

        verify(bookingRepository).deleteById("cancelled-1");
    }

    @Test
    void legacyDeleteRejectsNonCancelledBookings() {
        Booking active = savedBooking("active-1", "2026-06-20", "2026-06-23", "active");
        when(bookingRepository.findById("active-1")).thenReturn(Optional.of(active));

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.delCancelledBooking("active-1"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).deleteById("active-1");
    }

    @Test
    void legacyDeleteReturnsNotFoundForUnknownBookings() {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.delCancelledBooking("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bookingRepository, never()).deleteById("missing");
    }

    @Test
    void rejectsOverlappingRoomBookingWhenRoomFlagIsEnabled() {
        Booking existing = savedBooking("existing", "2026-06-20", "2026-06-23", "active");
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));
        when(bookingRepository.findByRoomAndStatusIn("APT2", BookingStatus.BOOKABLE))
              .thenReturn(List.of(existing));

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class,
              () -> bookingsService.create(booking("22/06/2026", "24/06/2026", "active")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void roomFlagCanAllowOverlapsWithoutDisablingVehicleProtection() {
        bookingsService = new BookingsService(
              bookingRepository, resourceService, new BookingAvailabilityPolicy(false, true), new BookingOverlapChecker(bookingRepository), new CurrentUserProvider(userRepository));
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking roomBooking = booking("22/06/2026", "24/06/2026", "active");
        assertNotNull(bookingsService.create(roomBooking).getId());
        verify(bookingRepository, never())
              .findByRoomAndStatusIn("APT2", BookingStatus.BOOKABLE);

        bookingsService = new BookingsService(
              bookingRepository, resourceService, new BookingAvailabilityPolicy(false, true), new BookingOverlapChecker(bookingRepository), new CurrentUserProvider(userRepository));
        Booking existingVehicleBooking = savedBooking("vehicle-existing", "2026-06-20", "2026-06-23", "paid");
        existingVehicleBooking.setRoom("CAR1");
        when(resourceService.requireActive("CAR1"))
              .thenReturn(new BookingResource("CAR1", "cars", "Car 1"));
        when(bookingRepository.findByRoomAndStatusIn("CAR1", BookingStatus.BOOKABLE))
              .thenReturn(List.of(existingVehicleBooking));

        Booking vehicleBooking = booking("22/06/2026", "24/06/2026", "active");
        vehicleBooking.setRoom("CAR1");
        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.create(vehicleBooking));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void cancelledBookingsDoNotBlockAvailability() {
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = booking("22/06/2026", "24/06/2026", "cancelled");
        assertNotNull(bookingsService.create(cancelled).getId());
        verify(bookingRepository, never())
              .findByRoomAndStatusIn("APT2", BookingStatus.BOOKABLE);
    }

    @Test
    void getAllReturnsBookingsWithoutMutatingDates() {
        Booking legacyBooking = savedBooking("legacy", "20/11/2026", "23/11/2026", "active");
        Booking isoBooking = savedBooking("iso", "2026-11-24", "2026-11-27", "active");
        when(bookingRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(legacyBooking, isoBooking));

        List<Booking> bookings = bookingsService.getAll();

        assertEquals(java.time.LocalDate.of(2026, 11, 20), bookings.get(0).getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 11, 23), bookings.get(0).getEndDate());
        assertEquals(java.time.LocalDate.parse("2026-11-24"), bookings.get(1).getStartDate());
        assertEquals(java.time.LocalDate.parse("2026-11-27"), bookings.get(1).getEndDate());
    }

    @Test
    void getAllForAuthenticatedUsersReturnsAllBookings() {
        User regular = new User();
        regular.setId(2);
        regular.setUsername("user");
        regular.setRole("OPERATOR");
        regular.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regular));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("user", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        Booking own = savedBooking("own", "2026-06-20", "2026-06-23", "active");
        own.setUser(regular);
        Booking other = savedBooking("other", "2026-06-24", "2026-06-26", "active");
        when(bookingRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(own, other));

        List<Booking> bookings = bookingsService.getAll();

        assertEquals(2, bookings.size());
        verify(bookingRepository).findAllByOrderByStartDateAsc();
        verify(bookingRepository, never()).findByUserIdOrderByStartDateAsc(2);
    }

    @Test
    void userCanUpdateAnyBooking() {
        User regular = new User();
        regular.setId(2);
        regular.setUsername("user");
        regular.setRole("OPERATOR");
        regular.setActive(true);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regular));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("user", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_OPERATOR")));

        Booking existing = savedBooking("booking-1", "2026-06-20", "2026-06-23", "active");
        existing.setUser(new User());
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(existing));
        when(resourceService.requireActive("APT2"))
              .thenReturn(new BookingResource("APT2", "rooms", "APT 2"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking update = booking("21/06/2026", "23/06/2026");
        update.setId("booking-1");

        Booking result = bookingsService.update(update);

        assertEquals(java.time.LocalDate.parse("2026-06-21"), result.getStartDate());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void adminCannotUpdateBookings() {
        Booking existing = savedBooking("booking-1", "2026-06-20", "2026-06-23", "active");
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(existing));

        Booking update = booking("21/06/2026", "23/06/2026");
        update.setId("booking-1");

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.update(update));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void guestCannotDeleteBookings() {
        User guest = new User();
        guest.setId(3);
        guest.setUsername("guest");
        guest.setRole("GUEST");
        guest.setActive(true);
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(guest));
        SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken("guest", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_GUEST")));

        Booking existing = savedBooking("booking-1", "2026-06-20", "2026-06-23", "active");
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> bookingsService.deleteObject("booking-1"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookingRepository, never()).deleteById("booking-1");
    }

    private static Booking booking(String startDate, String endDate) {
        return booking(startDate, endDate, null);
    }

    private static Booking booking(String startDate, String endDate, String status) {
        Booking booking = new Booking();
        booking.setStartDate(com.leonsoft.services.BookingsUtils.parseStoredDate(startDate));
        booking.setEndDate(com.leonsoft.services.BookingsUtils.parseStoredDate(endDate));
        booking.setCharge(new java.math.BigDecimal("100.0"));
        booking.setCommission(new java.math.BigDecimal("10.0"));
        booking.setReceived(new java.math.BigDecimal("20.0"));
        booking.setRoom("APT2");
        booking.setStatus(status == null ? null : com.leonsoft.models.BookingStatus.from(status));
        return booking;
    }

    private static Booking savedBooking(String id, String startDate, String endDate, String status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStartDate(com.leonsoft.services.BookingsUtils.parseStoredDate(startDate));
        booking.setEndDate(com.leonsoft.services.BookingsUtils.parseStoredDate(endDate));
        booking.setRoom("APT2");
        booking.setStatus(status == null ? null : com.leonsoft.models.BookingStatus.from(status));
        return booking;
    }
}
