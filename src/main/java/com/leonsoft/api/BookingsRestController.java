package com.leonsoft.api;

import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingsService;
import com.leonsoft.services.ReportMessages;
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

@RestController
@RequestMapping({"/api/v1/booking", "/booking"})
public class BookingsRestController {

    private static final Logger log = LoggerFactory.getLogger(BookingsRestController.class);

    private final BookingsService bookingsService;

    public BookingsRestController(BookingsService bookingsService) {
        this.bookingsService = bookingsService;
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
        bookingsService.exportReportToPDF(
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
        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);
        return bookingsService.report(
                fromDateEuroFmt, toDateEuroFmt, ReportMessages.supportedLocale(language));

    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    Booking create(@Valid @RequestBody BookingRequest request) {
        return bookingsService.create(request.toBooking());
    }

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    Booking update(@Valid @RequestBody BookingRequest request) {
        return bookingsService.update(request.toBooking());
    }


    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    Boolean deleteObject(@Valid @RequestBody BookingDeleteRequest request) {
        log.debug(" Delete    {}", request.id());
        bookingsService.deleteObject(request.id());
        return Boolean.TRUE;
    }

    @PostMapping(value = "/del")
    @PreAuthorize("hasRole('OPERATOR')")
    void delCancelledBooking(@RequestParam(name = "bid") String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingsService.delCancelledBooking(bookingId);
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'GUEST')")
    List<Booking> getAll() {
        return bookingsService.getAll();
    }
}