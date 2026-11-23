package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.models.User;
import com.leonsoft.repositories.BookingRepository;
import com.leonsoft.repositories.UserRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingReportService {

    private static final Logger log = LoggerFactory.getLogger(BookingReportService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingResourceService resourceService;

    public BookingReportService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BookingResourceService resourceService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
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

    public String report(String fromDateEuroFmt, String toDateEuroFmt, Locale locale) {
        log.debug("/booking/report fromDate={} toDate={}", fromDateEuroFmt, toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        List<Booking> all = getAllBookingsForReport().stream()
                .filter(booking -> {
                    LocalDate bookingStart = BookingsUtils.parseStoredDate(booking.getStartDate());
                    LocalDate bookingEnd = BookingsUtils.parseStoredDate(booking.getEndDate());
                    return !bookingStart.isBefore(stDate) && !bookingEnd.isAfter(edDate);
                })
                .toList();
        addResourceNames(all);

        return BookingsHtmlTemplate.report(fromDateEuroFmt, toDateEuroFmt, all, locale);
    }

    private List<Booking> getAllBookingsForReport() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }
        return bookingRepository.findAllByOrderByStartDateAsc();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private void addResourceNames(List<Booking> bookings) {
        Map<String, String> namesById = resourceService.namesById();
        for (Booking booking : bookings) {
            booking.setRoomName(namesById.getOrDefault(booking.getRoom(), booking.getRoom()));
        }
    }
}
