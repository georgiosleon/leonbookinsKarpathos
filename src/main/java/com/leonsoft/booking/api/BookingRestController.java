package com.leonsoft.booking.api;


import com.leonsoft.booking.models.Booking;
import com.leonsoft.booking.services.BookingService;
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
public class BookingRestController {

    @Autowired
    BookingService bookingService;

    @GetMapping("/booking/export/pdf")
    public void exportToPDF(HttpServletResponse response,
          @RequestParam(name = "fromDate") String fromDateEuroFmt,
          @RequestParam(name = "toDate") String toDateEuroFmt
    ) throws DocumentException, IOException {
        log.debug("/booking/export/pdf");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);
        bookingService.exportToPDF(response, fromDateEuroFmt, toDateEuroFmt);
    }


    @GetMapping(value = "/booking/report", produces = MediaType.TEXT_PLAIN_VALUE)
    String report(
          @RequestParam(name = "name", required = false) String name,
          @RequestParam(name = "fromDate") String fromDateEuroFmt,
          @RequestParam(name = "toDate") String toDateEuroFmt) {
        log.info("/booking/report");
        log.info("fromDate param  " + fromDateEuroFmt);
        log.info("toDate param  " + toDateEuroFmt);
        log.info("name param  " + name);

        if (name != null) {
//            name = name.replaceAll("\\s+", "");
            name = name.trim();
        }
        return bookingService.report(name, fromDateEuroFmt, toDateEuroFmt);

    }


    @PostMapping(value = "/booking/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    Booking create(@RequestBody Booking booking) {
        booking.setId(UUID.randomUUID().toString());
        log.debug(" SAVE   {}  ", booking);
        return bookingService.create(booking);
    }


    @PostMapping(value = "/booking/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    Boolean deleteObject(
          @RequestBody Booking input) {
        log.info("==============    getBookingFDatabase");

        Booking bookingFromDatabase = bookingService.getBookingFDatabase(input.getId());
        log.info("==============    getBookingFDatabase   found in database ");

        if (bookingFromDatabase != null
              && input.getPassword()!= null
              && !    input.getPassword().isEmpty()    //  NOT_NULL_OR_EMPTY
              && input.getPassword().equals(bookingFromDatabase.getPassword())) {

            log.info("==============    getBookingFDatabase   delete it ");
            log.info(" ============ Delete    {}", input.getId());

            return bookingService.deleteObject(input);
        }
        log.info("==============    getBookingFDatabase   NOT found ");
        return false;
    }



//    @GetMapping(value = "/booking/del")
//    void delCancelledBooking(@RequestParam(name = "bid") String bookingId) {
//        log.debug(" Delete  a cancelled   {}", bookingId);
//        bookingService.delCancelledBooking(bookingId);
//    }

    @GetMapping(value = "/booking/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    List<Booking> getAll() {
        return bookingService.getAll();
    }

}
