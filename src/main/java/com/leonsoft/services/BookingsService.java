package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Service class for booking business logic with role-based access control */
@Service
public class BookingsService {

    private static final Logger log = LoggerFactory.getLogger(BookingsService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingResourceService resourceService;
    private final BookingAvailabilityPolicy availabilityPolicy;
    private final BookingOverlapChecker overlapChecker;

    public BookingsService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BookingResourceService resourceService,
            BookingAvailabilityPolicy availabilityPolicy,
            BookingOverlapChecker overlapChecker) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
        this.availabilityPolicy = availabilityPolicy;
        this.overlapChecker = overlapChecker;
    }

    /** Get the currently authenticated user, or null if no user is logged in */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /** Operators can create/update/delete any booking; admins and guests cannot. */
    private boolean canModifyBooking(Booking booking) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.isOperator();
    }

    @Transactional
    public Booking create(Booking booking) {

        User currentUser = getCurrentUser();
        String generatedId = UUID.randomUUID().toString();

        log.debug("Creating booking by user: {}", currentUser != null ? currentUser.getUsername() : "unknown");
        booking.setId(generatedId);

        // Assign current user as the booking owner if authenticated.
        if (currentUser != null) {
            booking.setUser(currentUser);
        }

        Booking saved = saveWithNormalizedValues(booking);

        log.debug("Booking created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Booking update(Booking booking) {
        Booking existing = booking.getId() == null
                ? null
                : bookingRepository.findById(booking.getId()).orElse(null);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        }

        if (!canModifyBooking(existing)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this booking.");
        }

        log.debug("Updating booking");
        booking.setRequestDateTime(existing.getRequestDateTime());
        return saveWithNormalizedValues(booking);
    }

    @Transactional
    private Booking saveWithNormalizedValues(Booking booking) {
        BookingResource resource = resourceService.requireActive(booking.getRoom());
        LocalDate stDate = LocalDate.parse(booking.getStartDate(), BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(booking.getEndDate(), BookingsUtils.formatterEURO);

        if (edDate.isAfter(stDate)
                && overlapChecker.isBookable(booking.getStatus())
                && availabilityPolicy.preventsOverlap(resource.getType())
                && overlapChecker.hasOverlap(booking, stDate, edDate)) {
            String unavailableMessage = "rooms".equals(resource.getType())
                    ? "Room is not available for those dates."
                    : "Vehicle is not available for those dates.";
            throw new ResponseStatusException(HttpStatus.CONFLICT, unavailableMessage);
        }

        booking.setStartDate(stDate.format(DateTimeFormatter.ISO_DATE));
        booking.setEndDate(edDate.format(DateTimeFormatter.ISO_DATE));

        booking.setNumOfNights(Math.toIntExact(ChronoUnit.DAYS.between(stDate, edDate)));
        booking.setCharge(booking.getCharge() == null ? 0 : booking.getCharge());
        booking.setCommission(booking.getCommission() == null ? 0.0 : booking.getCommission());
        booking.setReceived(booking.getReceived() == null ? 0.0 : booking.getReceived());
        booking.setBalance(booking.getCharge() - booking.getReceived() - booking.getCommission());

        booking = bookingRepository.save(booking);
        booking.setRoomName(resource.getName());
        return booking;
    }

    public Boolean deleteObject(String bookingId) {
        log.debug("Delete booking {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));

        if (!canModifyBooking(booking)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this booking.");
        }

        bookingRepository.deleteById(bookingId);
        return true;
    }

    public void delCancelledBooking(String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        if (!"cancelled".equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only cancelled bookings can be deleted.");
        }

        if (!canModifyBooking(booking)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this booking.");
        }

        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> getAll() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }
        // All authenticated roles can view all bookings; mutation permissions differ.
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        addResourceNames(all);
        return all;
    }

    private void addResourceNames(List<Booking> bookings) {
        Map<String, String> namesById = resourceService.namesById();
        for (Booking booking : bookings) {
            booking.setRoomName(namesById.getOrDefault(booking.getRoom(), booking.getRoom()));
        }
    }
}