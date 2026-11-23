package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leonsoft.models.Booking;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class BookingsHtmlTemplateTest {

    @Test
    void escapesBookingValuesAndDoesNotEmitExecutableActions() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setStartDate(java.time.LocalDate.parse("2026-06-20"));
        booking.setEndDate(java.time.LocalDate.parse("2026-06-22"));
        booking.setStatus(com.leonsoft.models.BookingStatus.CANCELLED);
        booking.setResourceId("<svg onload='alert(1)'>");
        booking.setName("<img src=x onerror=alert(1)> & guest");
        booking.setAgency("\" onmouseover=\"alert(1)");
        booking.setCharge(new java.math.BigDecimal("100.0"));
        booking.setCommission(new java.math.BigDecimal("0.0"));
        booking.setReceived(new java.math.BigDecimal("0.0"));
        booking.setBalance(new java.math.BigDecimal("100.0"));
        booking.setNumOfGuests(1);
        booking.setNumOfNights(2);
        booking.setExtraInfo("<script>alert(1)</script>");

        String report = new BookingsHtmlTemplate().render("20/06/2026", "30/06/2026", List.of(BookingReportView.from(booking, booking.getResourceId(), null, null)), Locale.ENGLISH);

        assertTrue(report.contains("&lt;svg onload=&#39;alert(1)&#39;&gt;"));
        assertTrue(report.contains("&lt;img src=x onerror=alert(1)&gt; &amp; guest"));
        assertTrue(report.contains("&quot; onmouseover=&quot;alert(1)"));
        assertTrue(report.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(report.contains("<script>"));
        assertFalse(report.contains("onclick="));
        assertFalse(report.contains("<img"));
    }

    @Test
    void rendersGreekReportLabelsAndLocaleSpecificNumberFormatting() {
        Booking booking = new Booking();
        booking.setStartDate(java.time.LocalDate.parse("2026-06-20"));
        booking.setEndDate(java.time.LocalDate.parse("2026-06-22"));
        booking.setStatus(com.leonsoft.models.BookingStatus.CANCELLED);
        booking.setCharge(new java.math.BigDecimal("1234.5"));
        booking.setCommission(new java.math.BigDecimal("0.0"));
        booking.setReceived(new java.math.BigDecimal("0.0"));
        booking.setBalance(new java.math.BigDecimal("1234.5"));
        booking.setNumOfGuests(1);
        booking.setNumOfNights(2);

        String report = new BookingsHtmlTemplate().render(
              "20/06/2026", "30/06/2026", List.of(BookingReportView.from(booking, booking.getResourceId(), null, null)), Locale.forLanguageTag("el-GR"));

        assertTrue(report.contains("<th>Κατάσταση</th>"));
        assertTrue(report.contains(">Ακυρωμένη</td>"));
        assertTrue(report.contains("<h3>ΣΥΝΟΛΑ</h3>"));
        assertTrue(report.contains("1.234,50"));
        assertFalse(report.contains("/export/pdf"));
    }

    @Test
    void rendersFrenchLabelsWithoutAnInaccuratePdfLink() {
        String report = new BookingsHtmlTemplate().render(
              "20/06/2026", "30/06/2026", List.of(), Locale.FRANCE);

        assertTrue(report.contains("<th>Arrivée</th>"));
        assertTrue(report.contains("<h3>TOTAUX</h3>"));
        assertFalse(report.contains("/export/pdf"));
    }
}
