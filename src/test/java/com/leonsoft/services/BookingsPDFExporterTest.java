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
        booking.setStartDate("2026-11-20");
        booking.setEndDate("2026-11-29");
        booking.setStatus("cancelled");
        booking.setRoom("APT2");
        booking.setRoomName("Δωμάτιο");
        booking.setName("Κράτηση");
        booking.setNumOfGuests(2);
        booking.setNumOfNights(9);
        booking.setCharge(22000.0);
        booking.setReceived(0.0);
        booking.setBalance(22000.0);

        MockHttpServletResponse response = new MockHttpServletResponse();
        new BookingsPDFExporter(
              List.of(booking),
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
        List<Booking> bookings = new ArrayList<>();
        for (int index = 0; index < 70; index++) {
            Booking booking = new Booking();
            booking.setId("booking-" + index);
            booking.setStartDate("2026-06-20");
            booking.setEndDate("2026-06-22");
            booking.setStatus(index == 0 ? "cancelled" : "active");
            booking.setRoom("APT2");
            booking.setRoomName("APT 2");
            booking.setName("Guest " + index);
            booking.setAgency("abnb");
            booking.setNumOfGuests(2);
            booking.setNumOfNights(2);
            booking.setCharge(200.0);
            booking.setReceived(50.0);
            booking.setBalance(150.0);
            bookings.add(booking);
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
