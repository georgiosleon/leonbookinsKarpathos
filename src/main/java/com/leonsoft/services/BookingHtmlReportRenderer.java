package com.leonsoft.services;

import java.util.List;
import java.util.Locale;

@FunctionalInterface
public interface BookingHtmlReportRenderer {
    String render(String fromDateEuroFmt, String toDateEuroFmt, List<BookingReportView> bookings, Locale locale);
}
