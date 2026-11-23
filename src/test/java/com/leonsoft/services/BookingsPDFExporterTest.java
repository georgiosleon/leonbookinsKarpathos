package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingResource;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class BookingsPDFExporterTest {

    @Test
    void preservesGreekUnicodeAndPunctuationInPdfText() throws Exception {
        Booking booking = new Booking();
        booking.setStartDate(java.time.LocalDate.parse("2026-11-20"));
        booking.setEndDate(java.time.LocalDate.parse("2026-11-29"));
        booking.setStatus(com.leonsoft.models.BookingStatus.CANCELLED);
        booking.setResourceId("APT2");
        booking.setResourceName("Δωμάτιο");
        booking.setName("Κράτηση");
        booking.setNumOfGuests(2);
        booking.setNumOfNights(9);
        booking.setCharge(new java.math.BigDecimal("22000.0"));
        booking.setReceived(new java.math.BigDecimal("0.0"));
        booking.setBalance(new java.math.BigDecimal("22000.0"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        new BookingsPDFExporter(
              List.of(BookingReportView.from(booking, "Δωμάτιο", null, null)),
              List.of(new BookingResource("APT2", "rooms", "Δωμάτιο")),
              "rooms",
              LocalDate.of(2026, 11, 1),
              LocalDate.of(2026, 11, 30),
              Locale.forLanguageTag("el-GR"))
              .export(response);

        PdfReader reader = new PdfReader(response.getContentAsByteArray());
        try {
            String pageText = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(pageText.contains("Αναφορά κρατήσεων"));
            assertTrue(pageText.contains("Δωμάτιο"));
            assertTrue(pageText.contains("Ακυρωμένη"));
            assertTrue(pageText.contains("Κράτηση"));
            assertTrue(pageText.contains("—"));
            assertTrue(pageText.contains("–"));
            assertTrue(pageText.contains("€"));
        } finally {
            reader.close();
        }
    }

    @Test
    void exportsTheSummaryResourceBreakdownAndRepeatedBookingHeaders() throws Exception {
        List<BookingReportView> bookings = new ArrayList<>();
        for (int index = 0; index < 70; index++) {
            Booking booking = new Booking();
            booking.setId("booking-" + index);
            booking.setStartDate(java.time.LocalDate.parse("2026-06-20"));
            booking.setEndDate(java.time.LocalDate.parse("2026-06-22"));
            booking.setStatus(index == 0 ? com.leonsoft.models.BookingStatus.CANCELLED : com.leonsoft.models.BookingStatus.ACTIVE);
            booking.setResourceId("APT2");
            booking.setResourceName("APT 2");
            booking.setName("Guest " + index);
            booking.setAgency("abnb");
            booking.setNumOfGuests(2);
            booking.setNumOfNights(2);
            booking.setCharge(new java.math.BigDecimal("200.0"));
            booking.setReceived(new java.math.BigDecimal("50.0"));
            booking.setBalance(new java.math.BigDecimal("150.0"));
            bookings.add(BookingReportView.from(booking, "APT 2", null, null));
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        BookingsPDFExporter exporter = new BookingsPDFExporter(
              bookings,
              List.of(new BookingResource("APT2", "rooms", "APT 2")),
              "rooms",
              LocalDate.of(2026, 6, 1),
              LocalDate.of(2026, 6, 30),
              Locale.UK);

        exporter.export(response);

        PdfReader reader = new PdfReader(response.getContentAsByteArray());
        try {
            assertTrue(reader.getNumberOfPages() > 1);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String firstPage = extractor.getTextFromPage(1);
            String secondPage = extractor.getTextFromPage(2);
            StringBuilder allPages = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                allPages.append(extractor.getTextFromPage(page));
            }
            assertTrue(firstPage.contains("Booking report"));
            assertTrue(firstPage.contains("By resource"));
            assertTrue(firstPage.contains("Bookings in period"));
            assertTrue(firstPage.contains("Cancelled bookings"));
            assertTrue(firstPage.contains("APT 2"));
            assertTrue(secondPage.contains("Status"));
            assertTrue(allPages.toString().contains("Guest 69"));
        } finally {
            reader.close();
        }
    }
}
