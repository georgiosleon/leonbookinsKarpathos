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
        Double charge,
        Double received,
        Double commission,
        Double balance,
        String name,
        String agency,
        String room,
        String roomName,
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
        return from(booking, booking.getRoomName());
    }

    public static BookingResponse from(Booking booking, String roomName) {
        String resolvedRoomName = roomName == null || roomName.isBlank() ? booking.getRoom() : roomName;
        return new BookingResponse(
                booking.getId(),
                formatDate(booking.getStartDate()),
                formatDate(booking.getEndDate()),
                booking.getNumOfNights(),
                booking.getCharge(),
                booking.getReceived(),
                booking.getCommission(),
                booking.getBalance(),
                booking.getName(),
                booking.getAgency(),
                booking.getRoom(),
                resolvedRoomName,
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
                booking.getStatus(),
                booking.getRequestDateTime());
    }

    private static String formatDate(String date) {
        if (date == null || date.isBlank()) {
            return date;
        }
        LocalDate parsed = BookingsUtils.parseStoredDate(date);
        return BookingsUtils.formatterEURO.format(parsed);
    }
}
