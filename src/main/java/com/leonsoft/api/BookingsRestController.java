package com.leonsoft.api;

import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingReportService;
import com.leonsoft.services.BookingResourceService;
import com.leonsoft.services.BookingsService;
import com.leonsoft.services.ReportMessages;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking API.
 */
@RestController
@RequestMapping("/api/v1/booking")
public class BookingsRestController {

    private static final Logger log = LoggerFactory.getLogger(BookingsRestController.class);

    private final BookingsService bookingsService;
    private final BookingReportService bookingReportService;
    private final BookingResourceService bookingResourceService;

    public BookingsRestController(BookingsService bookingsService, BookingReportService bookingReportService, BookingResourceService bookingResourceService) {
        this.bookingsService = bookingsService;
        this.bookingReportService = bookingReportService;
        this.bookingResourceService = bookingResourceService;
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public void exportToPDF(
            HttpServletResponse response,
            @RequestParam(name = "fromDate") String fromDateEuroFmt,
            @RequestParam(name = "toDate") String toDateEuroFmt,
            @RequestParam(name = "type") String resourceType,
            @RequestParam(name = "lang", defaultValue = "en") String language)
            throws IOException {
        bookingReportService.exportReportToPDF(
                response,
                fromDateEuroFmt,
                toDateEuroFmt,
                resourceType,
                ReportMessages.supportedLocale(language));
    }

    @GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    String report(
            @RequestParam(name = "fromDate") String fromDateEuroFmt,
            @RequestParam(name = "toDate") String toDateEuroFmt,
            @RequestParam(name = "lang", defaultValue = "en") String language) {
        log.debug("/booking/report fromDate={} toDate={}", fromDateEuroFmt, toDateEuroFmt);
        return bookingReportService.report(
                fromDateEuroFmt, toDateEuroFmt, ReportMessages.supportedLocale(language));

    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return BookingResponse.from(bookingsService.create(request.toBooking()));
    }

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    BookingResponse update(@Valid @RequestBody BookingRequest request) {
        return BookingResponse.from(bookingsService.update(request.toBooking()));
    }


    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    Boolean deleteObject(@Valid @RequestBody BookingDeleteRequest request) {
        log.debug(" Delete    {}", request.id());
        bookingsService.deleteObject(request.id());
        return Boolean.TRUE;
    }

    @PostMapping(value = "/del")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    void delCancelledBooking(@RequestParam(name = "bid") String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingsService.delCancelledBooking(bookingId);
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'GUEST')")
    List<BookingResponse> getAll() {
        return bookingsService.getAll().stream().map(BookingResponse::from).toList();
    }
}