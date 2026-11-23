package com.leonsoft.services;

import java.util.List;
import java.util.Locale;

public interface BookingHtmlReportRenderer {
    String render(String fromDateEuroFmt, String toDateEuroFmt, List<BookingReportView> bookings, Locale locale);
}
