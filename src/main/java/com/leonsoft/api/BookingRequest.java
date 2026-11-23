package com.leonsoft.api;

import com.leonsoft.models.Booking;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;

public record BookingRequest(
      String id,
      @NotBlank String name,
      @NotNull @Positive Integer numOfGuests,
      @NotBlank String agency,
      @NotBlank @com.fasterxml.jackson.annotation.JsonAlias("room") String resourceId,
      String country,
      @NotBlank String startDate,
      @NotBlank String endDate,
      Integer numOfNights,
      @DecimalMin("0.0") BigDecimal charge,
      @DecimalMin("0.0") BigDecimal commission,
      @DecimalMin("0.0") BigDecimal received,
      BigDecimal balance,
      @NotBlank @Pattern(regexp = "active|paid|cancelled") String status,
      String identification,
      String afm,
      @Email String email,
      String tel,
      String voucher,
      String taxRefNumber,
      String invoiceNumber,
      String extraInfo,
      String platformReservationId) {

    private static final DateTimeFormatter DATE_FORMAT =
          DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    @AssertTrue(message = "Dates must be valid and departure must be after arrival.")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMAT);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMAT);
            long nights = ChronoUnit.DAYS.between(start, end);
            return nights > 0 && nights <= Integer.MAX_VALUE;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    public Booking toBooking() {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setName(name);
        booking.setNumOfGuests(numOfGuests);
        booking.setAgency(agency);
        booking.setResourceId(resourceId);
        booking.setCountry(country);
        booking.setStartDate(startDate == null ? null : com.leonsoft.services.BookingsUtils.parseEuroDate(startDate));
        booking.setEndDate(endDate == null ? null : com.leonsoft.services.BookingsUtils.parseEuroDate(endDate));
        booking.setNumOfNights(numOfNights);
        booking.setCharge(charge);
        booking.setCommission(commission);
        booking.setReceived(received);
        booking.setBalance(balance);
        booking.setStatus(com.leonsoft.models.BookingStatus.from(status));
        booking.setIdentification(identification);
        booking.setAfm(afm);
        booking.setEmail(email);
        booking.setTel(tel);
        booking.setVoucher(voucher);
        booking.setTaxRefNumber(taxRefNumber);
        booking.setInvoiceNumber(invoiceNumber);
        booking.setExtraInfo(extraInfo);
        booking.setPlatformReservationId(platformReservationId);
        return booking;
    }

}
