package com.leonsoft.services;


import com.leonsoft.models.Booking;
import com.leonsoft.repositories.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingsService {

    @Autowired
    protected BookingRepository bookingRepository;

    public void exportToPDF(HttpServletResponse response, String fromDateEuroFmt, String toDateEuroFmt
    ) throws DocumentException, IOException {

        log.debug("/booking/export/pdf");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        List<Booking> results = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=bookings_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);
        BookingsPDFExporter exporter = new BookingsPDFExporter(results);
        exporter.export(response);

    }

    public String report(String fromDateEuroFmt, String toDateEuroFmt) {

        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        List<Booking> all = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));

        return BookinsHtmlTemplate.report(fromDateEuroFmt, toDateEuroFmt, all);

    }


    public Booking create(Booking booking) {

        booking.setId(UUID.randomUUID().toString());

        log.debug(" SAVE   {}  ", booking);

        // convert from String to localDate
        LocalDate stDate = LocalDate.parse(booking.getStartDate(), BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(booking.getEndDate(), BookingsUtils.formatterEURO);

        // save  ISO String formate
        booking.setStartDate(stDate.format(DateTimeFormatter.ISO_DATE));
        booking.setEndDate(edDate.format(DateTimeFormatter.ISO_DATE));

        booking.setCharge(booking.getCharge() == null ? 0 : booking.getCharge());
        booking.setCommission(booking.getCommission() == null ? 0.0 : booking.getCommission());
        booking.setReceived(booking.getReceived() == null ? 0.0 : booking.getReceived());
        booking.setBalance(booking.getBalance() == null ? 0.0 : booking.getBalance());

        booking = bookingRepository.save(booking);

        // send back EURO format
        booking.setStartDate(BookingsUtils.formatterEURO.format(stDate));
        booking.setEndDate(BookingsUtils.formatterEURO.format(edDate));

        return booking;
    }

    public Boolean deleteObject(Booking booking) {
        log.debug(" Delete    {}", booking.getId());
        bookingRepository.deleteById(booking.getId());
        return true;
    }

    public void delCancelledBooking(String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        for (Booking booking : all) {
            LocalDate stDt = LocalDate.parse(booking.getStartDate(), BookingsUtils.formatterISO);
            booking.setStartDate(BookingsUtils.formatterEURO.format(stDt));
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), BookingsUtils.formatterISO);
            booking.setEndDate(BookingsUtils.formatterEURO.format(edDt));
        }
        return all;
    }

}
