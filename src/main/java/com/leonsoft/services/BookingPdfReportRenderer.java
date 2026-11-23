package com.leonsoft.services;

import com.leonsoft.models.BookingResource;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public interface BookingPdfReportRenderer {
    void export(
            List<BookingReportView> bookings,
            List<BookingResource> resources,
            String resourceType,
            LocalDate startDate,
            LocalDate endDate,
            Locale locale,
            HttpServletResponse response) throws DocumentException, IOException;
}
