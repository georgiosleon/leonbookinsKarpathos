package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.repositories.BookingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BookingOverlapChecker {

    private final BookingRepository bookingRepository;

    public BookingOverlapChecker(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean hasOverlap(Booking candidate, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByRoomAndStatusIn(candidate.getRoom(), List.of("active", "paid"))
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), candidate.getId()))
                .anyMatch(existing -> existing.getStartDate().compareTo(endDate.toString()) < 0
                        && startDate.toString().compareTo(existing.getEndDate()) < 0);
    }

    public boolean isBookable(String status) {
        return "active".equals(status) || "paid".equals(status);
    }
}
