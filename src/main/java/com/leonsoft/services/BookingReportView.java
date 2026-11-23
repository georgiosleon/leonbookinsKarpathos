package com.leonsoft.services;

import com.leonsoft.models.Booking;
import java.util.Objects;

public record BookingReportView(
        String id,
        String room,
        String roomName,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate,
        String name,
        String email,
        String country,
        String identification,
        String afm,
        String agency,
        String voucher,
        String taxRefNumber,
        String invoiceNumber,
        Integer numOfGuests,
        Integer numOfNights,
        java.math.BigDecimal charge,
        java.math.BigDecimal commission,
        java.math.BigDecimal received,
        java.math.BigDecimal balance,
        String tel,
        String status,
        String extraInfo) {

    public static BookingReportView from(Booking booking, String resourceName) {
        String resolvedName = resourceName == null || resourceName.isBlank()
                ? booking.getRoom()
                : resourceName;
        return new BookingReportView(
                booking.getId(),
                booking.getRoom(),
                resolvedName,
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getName(),
                booking.getEmail(),
                booking.getCountry(),
                booking.getIdentification(),
                booking.getAfm(),
                booking.getAgency(),
                booking.getVoucher(),
                booking.getTaxRefNumber(),
                booking.getInvoiceNumber(),
                Objects.requireNonNullElse(booking.getNumOfGuests(), 0),
                Objects.requireNonNullElse(booking.getNumOfNights(), 0),
                booking.getCharge(),
                booking.getCommission(),
                booking.getReceived(),
                booking.getBalance(),
                booking.getTel(),
                booking.getStatus() == null ? null : booking.getStatus().getValue(),
                booking.getExtraInfo());
    }
}
