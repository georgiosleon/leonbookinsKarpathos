package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

/** Service class for booking business logic with role-based access control */
@Service
public class BookingsService {

    private static final Logger log = LoggerFactory.getLogger(BookingsService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingResourceService resourceService;
    private final BookingAvailabilityPolicy availabilityPolicy;

    public BookingsService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BookingResourceService resourceService,
            BookingAvailabilityPolicy availabilityPolicy) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
        this.availabilityPolicy = availabilityPolicy;
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

    /** Check if the current user is an admin */
    private boolean isCurrentUserAdmin() {
        User user = getCurrentUser();
        return user != null && "admin".equals(user.getRole());
    }

    /** Check if the current user is the owner of a booking, or is admin */
    private boolean canModifyBooking(Booking booking) {
        User currentUser = getCurrentUser();
        // Admin can modify any booking
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return true;
        }
        // User can modify their own booking
        if (currentUser != null && booking.getUser() != null
                && Objects.equals(booking.getUser().getId(), currentUser.getId())) {
            return true;
        }
        return false;
    }

    /** Check if the current user can view a booking */
    private boolean canViewBooking(Booking booking) {
        User currentUser = getCurrentUser();
        // Admin can view any booking
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return true;
        }
        // User can view their own booking
        if (currentUser != null && booking.getUser() != null
                && Objects.equals(booking.getUser().getId(), currentUser.getId())) {
            return true;
        }
        // Guest can view all bookings (handled at controller level)
        return false;
    }

    public void exportReportToPDF(
            HttpServletResponse response,
            String fromDateEuroFmt,
            String toDateEuroFmt,
            String resourceType,
            Locale locale) throws DocumentException, IOException {

        if (!"rooms".equals(resourceType) && !"cars".equals(resourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report resource type.");
        }

        LocalDate startDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate endDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report date range.");
        }

        // Filter bookings based on user role
        List<Booking> allBookings = getAllBookingsForReport();

        List<BookingResource> resources = resourceService.getAll().stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .toList();
        Map<String, BookingResource> resourcesById = resources.stream()
                .collect(java.util.stream.Collectors.toMap(BookingResource::getId, resource -> resource));

        List<Booking> bookings = allBookings.stream()
                .filter(booking -> resourcesById.containsKey(booking.getRoom()))
                .sorted(java.util.Comparator
                        .comparing(Booking::getStartDate)
                        .thenComparing(
                                booking -> Objects.toString(booking.getName(), ""),
                                Collator.getInstance(locale)))
                .toList();
        addResourceNames(bookings);

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=booking-report-" + resourceType + "-" + LocalDate.now() + ".pdf");
        new BookingsPDFExporter(bookings, resources, resourceType, startDate, endDate, locale)
                .export(response);
    }

    /** Get all bookings visible to the current user based on their role */
    private List<Booking> getAllBookingsForReport() {
        User currentUser = getCurrentUser();
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            // Admin sees all bookings
            return bookingRepository.findAllByOrderByStartDateAsc();
        }
        // User sees only their own bookings
        if (currentUser != null) {
            return bookingRepository.findByUserIdOrderByStartDateAsc(currentUser.getId());
        }
        // No authenticated user - return empty (will be handled by authentication filter)
        return List.of();
    }

    public String report(String fromDateEuroFmt, String toDateEuroFmt, Locale locale) {

        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        // Use role-aware filtering
        List<Booking> all = getAllBookingsForReport().stream()
                .filter(booking -> {
                    LocalDate bookingStart = BookingsUtils.parseStoredDate(booking.getStartDate());
                    LocalDate bookingEnd = BookingsUtils.parseStoredDate(booking.getEndDate());
                    return !bookingStart.isBefore(stDate) && !bookingEnd.isAfter(edDate);
                })
                .toList();
        addResourceNames(all);

        return BookinsHtmlTemplate.report(fromDateEuroFmt, toDateEuroFmt, all, locale);

    }

    @Transactional
    public Booking create(Booking booking) {

        User currentUser = getCurrentUser();
        String generatedId = UUID.randomUUID().toString();

        log.debug("Creating booking by user: {}", currentUser != null ? currentUser.getUsername() : "unknown");
        booking.setId(generatedId);

        // Assign current user as the booking owner if authenticated
        if (currentUser != null) {
            User user = userRepository.findById(currentUser.getId())
                    .orElseGet(() -> {
                        // Save the user if not found (shouldn't happen if auth works)
                        User u = new User();
                        u.setId(currentUser.getId());
                        u.setUsername(currentUser.getUsername());
                        u.setRole(currentUser.getRole());
                        u.setActive(true);
                        return userRepository.save(u);
                    });
            booking.setUser(user);
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
                && isBookable(booking.getStatus())
                && availabilityPolicy.preventsOverlap(resource.getType())
                && hasOverlappingBooking(booking, stDate, edDate)) {
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

        booking.setStartDate(BookingsUtils.formatterEURO.format(stDate));
        booking.setEndDate(BookingsUtils.formatterEURO.format(edDate));

        return booking;
    }

    private boolean hasOverlappingBooking(Booking candidate, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByRoomAndStatusIn(
                        candidate.getRoom(), List.of("active", "paid"))
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), candidate.getId()))
                .anyMatch(existing -> existing.getStartDate().compareTo(endDate.toString()) < 0
                        && startDate.toString().compareTo(existing.getEndDate()) < 0);
    }

    private static boolean isBookable(String status) {
        return "active".equals(status) || "paid".equals(status);
    }

    public Boolean deleteObject(String bookingId) {
        log.debug(" Delete    {}", bookingId);
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        }

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
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            // Admin sees all bookings
            List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
            addResourceNames(all);
            for (Booking booking : all) {
                LocalDate stDt = BookingsUtils.parseStoredDate(booking.getStartDate());
                booking.setStartDate(BookingsUtils.formatterEURO.format(stDt));
                LocalDate edDt = BookingsUtils.parseStoredDate(booking.getEndDate());
                booking.setEndDate(BookingsUtils.formatterEURO.format(edDt));
            }
            return all;
        }
        // User sees only their own bookings
        if (currentUser != null) {
            List<Booking> userBookings = bookingRepository.findByUserIdOrderByStartDateAsc(currentUser.getId());
            addResourceNames(userBookings);
            for (Booking booking : userBookings) {
                LocalDate stDt = BookingsUtils.parseStoredDate(booking.getStartDate());
                booking.setStartDate(BookingsUtils.formatterEURO.format(stDt));
                LocalDate edDt = BookingsUtils.parseStoredDate(booking.getEndDate());
                booking.setEndDate(BookingsUtils.formatterEURO.format(edDt));
            }
            return userBookings;
        }
        // No authenticated user - return empty
        return List.of();
    }

    private void addResourceNames(List<Booking> bookings) {
        Map<String, String> namesById = resourceService.namesById();
        for (Booking booking : bookings) {
            booking.setRoomName(namesById.getOrDefault(booking.getRoom(), booking.getRoom()));
        }
    }
}