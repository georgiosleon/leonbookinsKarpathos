package com.leonsoft.api;

import com.leonsoft.services.BookingAvailabilityPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class AvailabilitySettingsRestController {

    private final BookingAvailabilityPolicy availabilityPolicy;

    public AvailabilitySettingsRestController(BookingAvailabilityPolicy availabilityPolicy) {
        this.availabilityPolicy = availabilityPolicy;
    }

    @GetMapping("/availability")
    public BookingAvailabilityPolicy.AvailabilitySettings availability() {
        return availabilityPolicy.settings();
    }
}
