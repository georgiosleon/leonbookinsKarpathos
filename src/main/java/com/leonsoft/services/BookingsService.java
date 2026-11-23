package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.BookingStatus;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Service class for booking business logic with role-based access control */
@Service
public class BookingsService {

    private static final Logger log = LoggerFactory.getLogger(BookingsService.class);

    private final BookingRepository bookingRepository;
    private final BookingResourceService resourceService;
    private final BookingAvailabilityPolicy availabilityPolicy;
    private final BookingOverlapChecker overlapChecker;
    private final CurrentUserProvider currentUserProvider;

    public BookingsService(
            BookingRepository bookingRepository,
            BookingResourceService resourceService,
            BookingAvailabilityPolicy availabilityPolicy,
            BookingOverlapChecker overlapChecker,
            CurrentUserProvider currentUserProvider) {
        this.bookingRepository = bookingRepository;
        this.resourceService = resourceService;
        this.availabilityPolicy = availabilityPolicy;
        this.overlapChecker = overlapChecker;
        this.currentUserProvider = currentUserProvider;
    }

    /** Operators can create/update/delete any booking; admins and guests cannot. */
    private boolean canModifyBooking(Booking booking) {
        User currentUser = currentUserProvider.getCurrentUser().orElse(null);
        return currentUser != null && currentUser.isOperator();
    }

    @Transactional
    public Booking create(Booking booking) {

        User currentUser = currentUserProvider.getCurrentUser().orElse(null);
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
        LocalDate stDate = BookingsUtils.parseEuroDate(booking.getStartDate());
        LocalDate edDate = BookingsUtils.parseEuroDate(booking.getEndDate());

        if (edDate.isAfter(stDate)
                && overlapChecker.isBookable(booking.getStatus())
                && availabilityPolicy.preventsOverlap(resource.getType())
                && overlapChecker.hasOverlap(booking, stDate, edDate)) {
            String unavailableMessage = com.leonsoft.models.BookingResourceType.ROOMS.equals(resource.getType())
                    ? "Room is not available for those dates."
                    : "Vehicle is not available for those dates.";
            throw new ResponseStatusException(HttpStatus.CONFLICT, unavailableMessage);
        }

        booking.setStartDate(stDate.format(DateTimeFormatter.ISO_DATE));
        booking.setEndDate(edDate.format(DateTimeFormatter.ISO_DATE));

        booking.setNumOfNights(Math.toIntExact(ChronoUnit.DAYS.between(stDate, edDate)));
        booking.setCharge(BookingsUtils.normalizeMoney(booking.getCharge()));
        booking.setCommission(BookingsUtils.normalizeMoney(booking.getCommission()));
        booking.setReceived(BookingsUtils.normalizeMoney(booking.getReceived()));
        booking.setBalance(BookingsUtils.calculateBalance(
                booking.getCharge(), booking.getReceived(), booking.getCommission()));

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
        log.debug("Delete cancelled booking {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        if (!BookingStatus.isCancelled(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only cancelled bookings can be deleted.");
        }

        if (!canModifyBooking(booking)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this booking.");
        }

        bookingRepository.deleteById(bookingId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAll() {
        User currentUser = currentUserProvider.getCurrentUser().orElse(null);
        if (currentUser == null) {
            return List.of();
        }
        // All authenticated roles can view all bookings; mutation permissions differ.
        return bookingRepository.findAllByOrderByStartDateAsc();
    }
}