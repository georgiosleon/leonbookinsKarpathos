package com.leonsoft.api;


import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingsService;
import com.leonsoft.services.ReportMessages;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping({"/api/v1/booking", "/booking"})
public class BookingsRestController {

    private final BookingsService bookingsService;

    public BookingsRestController(BookingsService bookingsService) {
        this.bookingsService = bookingsService;
    }

    @GetMapping("/export/pdf")
    public void exportToPDF(
          HttpServletResponse response,
          @RequestParam(name = "fromDate") String fromDateEuroFmt,
          @RequestParam(name = "toDate") String toDateEuroFmt,
          @RequestParam(name = "type") String resourceType,
          @RequestParam(name = "lang", defaultValue = "en") String language)
          throws DocumentException, IOException {
        bookingsService.exportReportToPDF(
              response,
              fromDateEuroFmt,
              toDateEuroFmt,
              resourceType,
              ReportMessages.supportedLocale(language));
    }

    @GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
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
    Booking create(@Valid @RequestBody BookingRequest request) {
        return bookingsService.create(request.toBooking());
    }

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    Booking update(@Valid @RequestBody BookingRequest request) {
        return bookingsService.update(request.toBooking());
    }


    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    Boolean deleteObject(@Valid @RequestBody BookingDeleteRequest request) {
        log.debug(" Delete    {}", request.id());
        bookingsService.deleteObject(request.id());
        return Boolean.TRUE;
    }

    @PostMapping(value = "/del")
    void delCancelledBooking(@RequestParam(name = "bid") String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingsService.delCancelledBooking(bookingId);
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    List<Booking> getAll() {
        return bookingsService.getAll();
    }

}
