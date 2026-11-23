package com.leonsoft.api;

import com.leonsoft.models.Booking;
import com.leonsoft.services.BookingsUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        String id,
        String startDate,
        String endDate,
        Integer numOfNights,
        java.math.BigDecimal charge,
        java.math.BigDecimal received,
        java.math.BigDecimal commission,
        java.math.BigDecimal rentalTax,
        java.math.BigDecimal balance,
        String name,
        String agency,
        String resourceId,
        String resourceName,
        Integer numOfGuests,
        String country,
        String voucher,
        String identification,
        String invoiceNumber,
        String taxRefNumber,
        String email,
        String afm,
        String tel,
        String extraInfo,
        String status,
        LocalDateTime requestDateTime) {

    public static BookingResponse from(Booking booking) {
        return from(booking, booking.getResourceName());
    }

    public static BookingResponse from(Booking booking, String resourceName) {
        String resolvedResourceName = resourceName == null || resourceName.isBlank() ? booking.getResourceId() : resourceName;
        return new BookingResponse(
                booking.getId(),
                formatDate(booking.getStartDate()),
                formatDate(booking.getEndDate()),
                booking.getNumOfNights(),
                booking.getCharge(),
                booking.getReceived(),
                booking.getCommission(),
                booking.getRentalTax(),
                booking.getBalance(),
                booking.getName(),
                booking.getAgency(),
                booking.getResourceId(),
                resolvedResourceName,
                booking.getNumOfGuests(),
                booking.getCountry(),
                booking.getVoucher(),
                booking.getIdentification(),
                booking.getInvoiceNumber(),
                booking.getTaxRefNumber(),
                booking.getEmail(),
                booking.getAfm(),
                booking.getTel(),
                booking.getExtraInfo(),
                booking.getStatus() == null ? null : booking.getStatus().getValue(),
                booking.getRequestDateTime());
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return BookingsUtils.formatterEURO.format(date);
    }
}
