package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingStatus;
import com.leonsoft.repositories.BookingRepository;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BookingOverlapChecker {

    private final BookingRepository bookingRepository;

    public BookingOverlapChecker(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean hasOverlap(Booking candidate, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByRoomAndStatusIn(candidate.getRoom(), BookingStatus.BOOKABLE)
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), candidate.getId()))
                .anyMatch(existing -> existing.getStartDate().compareTo(endDate.toString()) < 0
                        && startDate.toString().compareTo(existing.getEndDate()) < 0);
    }

    public boolean isBookable(String status) {
        return BookingStatus.isBookable(status);
    }
}
