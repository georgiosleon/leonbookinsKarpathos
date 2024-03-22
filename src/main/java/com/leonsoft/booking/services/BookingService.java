package com.leonsoft.booking.services;


import com.leonsoft.booking.models.Booking;
import com.leonsoft.booking.repositories.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingService {

    @Autowired
    protected BookingRepository bookingRepository;

    public void exportToPDF(HttpServletResponse response, String fromDateEuroFmt, String toDateEuroFmt
    ) throws DocumentException, IOException {

        log.debug("/booking/export/pdf");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, DateTimeService.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, DateTimeService.formatterEURO);

//        List<Booking> results = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
//              stDate.format(DateTimeFormatter.ISO_DATE),
//              edDate.format(DateTimeFormatter.ISO_DATE));

        List<Booking> results = bookingRepository.findAllBookingsForReport(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=bookings_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);
        BookingPDFExporter exporter = new BookingPDFExporter(results);
        exporter.export(response);

    }

    public String report(String name, String fromDateEuroFmt, String toDateEuroFmt) {

        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, DateTimeService.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, DateTimeService.formatterEURO);

        List<Booking> results;

        if (name == null || name.isBlank()) {
            results = bookingRepository
                  .findAllBookingsForReport(stDate.format(DateTimeFormatter.ISO_DATE), edDate.format(DateTimeFormatter.ISO_DATE));
        } else {
            results = bookingRepository.findByNameContainingIgnoreCase(name);

        }

        return BookingHtmlTemplate.report(name, fromDateEuroFmt, toDateEuroFmt, results);

    }

    boolean validateBooking(Booking booking) {

        if (booking.getName() == null) {
            return false;
        }
        if (booking.getExtraInfo() == null) {
            return false;
        }
        if (booking.getPassword() == null ||  booking.getPassword().length() < 3 )  {
            return false;
        }

//
        //  TODO  add as parameters  in applications.props
        booking.setName( Jsoup.clean( booking.getName() , Safelist.basic() ) );
        booking.setExtraInfo( Jsoup.clean( booking.getExtraInfo() , Safelist.basic() ) );
        // option 1        booking.setExtraInfo(Jsoup.parse(booking.getExtraInfo()).text());
        // option 2       booking.setName(Jsoup.parse(booking.getName()).text());


        return true;
    }


    public Booking create(Booking booking) {

        if (!validateBooking(booking)) {
            return null;
        }

        booking = fillBooking(booking);

        return booking;
    }


    public Booking getBookingFDatabase(String id) {
        Optional<Booking> obj = bookingRepository.findById(id);
        return obj.orElse(null);

    }


    private Booking fillBooking(Booking booking) {
        booking.setId(UUID.randomUUID().toString());

        log.debug(" SAVE   {}  ", booking);

        // convert from String to localDate
        LocalDate stDate = LocalDate.parse(booking.getStartDate(), DateTimeService.formatterEURO);
        LocalDate edDate = LocalDate.parse(booking.getEndDate(), DateTimeService.formatterEURO);

        // save  ISO String formate
        booking.setStartDate(stDate.format(DateTimeFormatter.ISO_DATE));
        booking.setEndDate(edDate.format(DateTimeFormatter.ISO_DATE));

        booking.setCharge(booking.getCharge() == null ? 0 : booking.getCharge());
        booking.setCommission(booking.getCommission() == null ? 0.0 : booking.getCommission());
        booking.setReceived(booking.getReceived() == null ? 0.0 : booking.getReceived());
        booking.setBalance(booking.getBalance() == null ? 0.0 : booking.getBalance());

        booking = bookingRepository.save(booking);

        // send back EURO format
        booking.setStartDate(DateTimeService.formatterEURO.format(stDate));
        booking.setEndDate(DateTimeService.formatterEURO.format(edDate));
        return booking;
    }

    public Boolean deleteObject(Booking booking) {
        log.debug(" Delete    {}", booking.getId());
        bookingRepository.deleteById(booking.getId());
        return true;
    }

//    public void delCancelledBooking(String bookingId) {
//        log.debug(" Delete  a cancelled   {}", bookingId);
//        bookingRepository.deleteById(bookingId);
//    }

    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        for (Booking booking : all) {
            LocalDate stDt = LocalDate.parse(booking.getStartDate(), DateTimeService.formatterISO);
            booking.setStartDate(DateTimeService.formatterEURO.format(stDt));
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), DateTimeService.formatterISO);
            booking.setEndDate(DateTimeService.formatterEURO.format(edDt));
            // todo fix
            booking.setPassword("??????");

        }
        return all;
    }

}
