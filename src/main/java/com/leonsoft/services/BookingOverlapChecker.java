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
        return bookingRepository.findByResourceIdAndStatusIn(candidate.getResourceId(), BookingStatus.BOOKABLE)
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), candidate.getId()))
                .anyMatch(existing -> existing.getStartDate().isBefore(endDate)
                        && startDate.isBefore(existing.getEndDate()));
    }

    public boolean isBookable(BookingStatus status) {
        return BookingStatus.isBookable(status);
    }
}
