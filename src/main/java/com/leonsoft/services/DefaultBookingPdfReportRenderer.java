package com.leonsoft.services;

import com.leonsoft.models.BookingResource;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DefaultBookingPdfReportRenderer implements BookingPdfReportRenderer {

    @Override
    public void export(
            List<BookingReportView> bookings,
            List<BookingResource> resources,
            String resourceType,
            LocalDate startDate,
            LocalDate endDate,
            Locale locale,
            HttpServletResponse response) throws DocumentException, IOException {
        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=booking-report-" + resourceType + "-" + LocalDate.now() + ".pdf");
        new BookingsPDFExporter(bookings, resources, resourceType, startDate, endDate, locale)
                .export(response);
    }
}
