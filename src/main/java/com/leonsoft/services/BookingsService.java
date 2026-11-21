package com.leonsoft.services;


import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class BookingsService {

    private final BookingRepository bookingRepository;
    private final BookingResourceService resourceService;
    private final BookingAvailabilityPolicy availabilityPolicy;

    public BookingsService(
          BookingRepository bookingRepository,
          BookingResourceService resourceService,
          BookingAvailabilityPolicy availabilityPolicy) {
        this.bookingRepository = bookingRepository;
        this.resourceService = resourceService;
        this.availabilityPolicy = availabilityPolicy;
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

        List<BookingResource> resources = resourceService.getAll().stream()
              .filter(resource -> resourceType.equals(resource.getType()))
              .toList();
        Map<String, BookingResource> resourcesById = resources.stream()
              .collect(java.util.stream.Collectors.toMap(BookingResource::getId, resource -> resource));

        List<Booking> bookings = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
                    startDate.format(DateTimeFormatter.ISO_DATE),
                    endDate.format(DateTimeFormatter.ISO_DATE))
              .stream()
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


    @Transactional
    public Booking create(Booking booking) {

        booking.setId(UUID.randomUUID().toString());

        log.debug("Creating booking");
        return saveWithNormalizedValues(booking);
    }

    @Transactional
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

        if (edDate.isAfter(stDate)
              && isBookable(booking.getStatus())
              && availabilityPolicy.preventsOverlap(resource.getType())
              && hasOverlappingBooking(booking, stDate, edDate)) {
            String unavailableMessage = "rooms".equals(resource.getType())
                  ? "Room is not available for those dates."
                  : "Vehicle is not available for those dates.";
            throw new ResponseStatusException(HttpStatus.CONFLICT, unavailableMessage);
        }

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

    private boolean hasOverlappingBooking(Booking candidate, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByRoomAndStatusIn(
                    candidate.getRoom(), List.of("active", "paid"))
              .stream()
              .filter(existing -> !Objects.equals(existing.getId(), candidate.getId()))
              .anyMatch(existing -> existing.getStartDate().compareTo(endDate.toString()) < 0
                    && startDate.toString().compareTo(existing.getEndDate()) < 0);
    }

    private static boolean isBookable(String status) {
        return "active".equals(status) || "paid".equals(status);
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
        Booking booking = bookingRepository.findById(bookingId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        if (!"cancelled".equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only cancelled bookings can be deleted.");
        }
        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        addResourceNames(all);
        for (Booking booking : all) {
            LocalDate stDt = BookingsUtils.parseStoredDate(booking.getStartDate());
            booking.setStartDate(BookingsUtils.formatterEURO.format(stDt));
            LocalDate edDt = BookingsUtils.parseStoredDate(booking.getEndDate());
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
