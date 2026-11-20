package com.leonsoft.services;


import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class BookingsService {

    private final BookingRepository bookingRepository;
    private final BookingResourceService resourceService;

    public BookingsService(
          BookingRepository bookingRepository, BookingResourceService resourceService) {
        this.bookingRepository = bookingRepository;
        this.resourceService = resourceService;
    }

    public void exportToPDF(
          HttpServletResponse response, String fromDateEuroFmt, String toDateEuroFmt, Locale locale
    ) throws DocumentException, IOException {

        log.debug("/booking/export/pdf");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        List<Booking> results = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));
        addResourceNames(results);

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=bookings_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);
        BookingsPDFExporter exporter = new BookingsPDFExporter(results, locale);
        exporter.export(response);

    }

    public String report(String fromDateEuroFmt, String toDateEuroFmt, Locale locale) {

        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, BookingsUtils.formatterEURO);

        List<Booking> all = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));
        addResourceNames(all);

        return BookinsHtmlTemplate.report(fromDateEuroFmt, toDateEuroFmt, all, locale);

    }


    public Booking create(Booking booking) {

        booking.setId(UUID.randomUUID().toString());

        log.debug("Creating booking");
        return saveWithNormalizedValues(booking);
    }

    public Booking update(Booking booking) {
        Booking existing = booking.getId() == null
              ? null
              : bookingRepository.findById(booking.getId()).orElse(null);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        }

        log.debug("Updating booking");
        booking.setRequestDateTime(existing.getRequestDateTime());
        return saveWithNormalizedValues(booking);
    }

    private Booking saveWithNormalizedValues(Booking booking) {
        BookingResource resource = resourceService.requireActive(booking.getRoom());
        LocalDate stDate = LocalDate.parse(booking.getStartDate(), BookingsUtils.formatterEURO);
        LocalDate edDate = LocalDate.parse(booking.getEndDate(), BookingsUtils.formatterEURO);

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

    public Boolean deleteObject(String bookingId) {
        log.debug(" Delete    {}", bookingId);
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        }
        bookingRepository.deleteById(bookingId);
        return true;
    }

    public void delCancelledBooking(String bookingId) {
        log.debug(" Delete  a cancelled   {}", bookingId);
        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        addResourceNames(all);
        for (Booking booking : all) {
            LocalDate stDt = LocalDate.parse(booking.getStartDate(), BookingsUtils.formatterISO);
            booking.setStartDate(BookingsUtils.formatterEURO.format(stDt));
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), BookingsUtils.formatterISO);
            booking.setEndDate(BookingsUtils.formatterEURO.format(edDt));
        }
        return all;
    }

    private void addResourceNames(List<Booking> bookings) {
        Map<String, String> namesById = resourceService.namesById();
        for (Booking booking : bookings) {
            booking.setRoomName(namesById.getOrDefault(booking.getRoom(), booking.getRoom()));
        }
    }

}
