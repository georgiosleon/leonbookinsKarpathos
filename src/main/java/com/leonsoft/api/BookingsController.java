package com.leonsoft.api;


import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingsService;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BookingsController {

    @Autowired
    BookingsService bookingsService;

    @GetMapping("/booking/export/pdf")
    public void exportToPDF(HttpServletResponse response,
          @RequestParam(name = "fromDate") String fromDateEuroFmt,
          @RequestParam(name = "toDate") String toDateEuroFmt
    ) throws DocumentException, IOException {
        log.debug("/booking/export/pdf");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);
        bookingsService.exportToPDF(response, fromDateEuroFmt, toDateEuroFmt);
    }


    @GetMapping(value = "/booking/report", produces = MediaType.TEXT_PLAIN_VALUE)
    String report(
          @RequestParam(name = "fromDate") String fromDateEuroFmt,
          @RequestParam(name = "toDate") String toDateEuroFmt) {
        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);
        return bookingsService.report(fromDateEuroFmt, toDateEuroFmt).toString();

    }


    @PostMapping(value = "/booking/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    Booking create(@RequestBody Booking booking) {
        booking.setId(UUID.randomUUID().toString());
        log.debug(" SAVE   {}  ", booking);
        return bookingsService.create(booking);
    }


    @PostMapping(value = "/booking/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    Boolean deleteObject(@RequestBody Booking booking) {
        log.debug(" Delete    {}", booking.getId());
        bookingsService.deleteObject(booking);
        return true;
    }

    @GetMapping(value = "/booking/del")
    void delCancelledBooking(@RequestParam(name = "bid") String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingsService.delCancelledBooking(bookingId);
    }

    @GetMapping(value = "/booking/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    List<Booking> getAll() {
        return bookingsService.getAll();
    }

}
