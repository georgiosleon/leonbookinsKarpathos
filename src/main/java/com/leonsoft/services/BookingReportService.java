package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingReportService {

    private static final Logger log = LoggerFactory.getLogger(BookingReportService.class);

    private final BookingRepository bookingRepository;
    private final BookingResourceService resourceService;
    private final CurrentUserProvider currentUserProvider;

    public BookingReportService(
            BookingRepository bookingRepository,
            BookingResourceService resourceService,
            CurrentUserProvider currentUserProvider) {
        this.bookingRepository = bookingRepository;
        this.resourceService = resourceService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public void exportReportToPDF(
            HttpServletResponse response,
            String fromDateEuroFmt,
            String toDateEuroFmt,
            String resourceType,
            Locale locale) throws DocumentException, IOException {

        if (!com.leonsoft.models.BookingResourceType.isSupported(resourceType)) {
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
                .collect(Collectors.toMap(BookingResource::getId, resource -> resource));

        List<Booking> bookings = allBookings.stream()
                .filter(booking -> resourcesById.containsKey(booking.getRoom()))
                .sorted(Comparator
                        .comparing(Booking::getStartDate)
                        .thenComparing(
                                booking -> Objects.toString(booking.getName(), ""),
                                Collator.getInstance(locale)))
                .toList();
        resourceService.populateResourceNames(bookings);

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=booking-report-" + resourceType + "-" + LocalDate.now() + ".pdf");
        new BookingsPDFExporter(bookings, resources, resourceType, startDate, endDate, locale)
                .export(response);
    }

    @Transactional(readOnly = true)
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
        resourceService.populateResourceNames(all);

        return BookingsHtmlTemplate.report(fromDateEuroFmt, toDateEuroFmt, all, locale);
    }

    private List<Booking> getAllBookingsForReport() {
        return currentUserProvider.getCurrentUser()
                .map(user -> bookingRepository.findAllByOrderByStartDateAsc())
                .orElse(List.of());
    }
}
