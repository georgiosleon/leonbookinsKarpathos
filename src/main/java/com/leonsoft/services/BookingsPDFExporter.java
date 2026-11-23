package com.leonsoft.services;

import com.leonsoft.models.BookingStatus;
import com.leonsoft.models.BookingResource;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class BookingsPDFExporter {
    private static final List<String> BOOKING_HEADERS = List.of(
            "status", "arrival", "departure", "resource", "name", "agency",
            "guests", "nights", "rentalTaxPerNight", "charge", "received", "rentalTax", "balance");

    private final List<BookingReportView> bookings;
    private final List<BookingResource> resources;
    private final String resourceType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Locale locale;
    private final DateTimeFormatter dateFormatter;
    private final NumberFormat moneyFormat;
    private BaseFont reportBaseFont;

    public BookingsPDFExporter(
          List<BookingReportView> bookings,
          List<BookingResource> resources,
          String resourceType,
          LocalDate startDate,
          LocalDate endDate,
          Locale locale) {
        this.bookings = bookings;
        this.resources = resources;
        this.resourceType = resourceType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.locale = locale;
        this.dateFormatter = DateTimeFormatter.ofPattern(datePattern(locale), locale);
        this.moneyFormat = NumberFormat.getCurrencyInstance(locale);
        this.moneyFormat.setCurrency(Currency.getInstance("EUR"));
    }

    public void export(HttpServletResponse response) throws DocumentException, IOException {
        reportBaseFont = createReportBaseFont();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 24, 20);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font titleFont = reportFont(14);
        Paragraph title = new Paragraph(
              text("title") + " — " + text(resourceType) + " — "
                    + dateFormatter.format(startDate) + " – " + dateFormatter.format(endDate),
              titleFont);
        title.setSpacingAfter(10);
        document.add(title);

        addSummary(document);
        addResourceSummary(document);
        addBookings(document);

        document.close();
    }

    private void addSummary(Document document) throws DocumentException {
        List<BookingReportView> bookable = bookings.stream().filter(this::isBookable).toList();
        long cancelled = bookings.stream().filter(booking -> BookingStatus.isCancelled(booking.status())).count();
        int guests = sumInt(bookable, BookingReportView::numOfGuests);
        int nights = sumInt(bookable, BookingReportView::numOfNights);

        PdfPTable summary = new PdfPTable(8);
        summary.setWidthPercentage(100);
        summary.setWidths(new float[] {12, 12, 10, 10, 14, 14, 14, 14});
        addSummaryCell(summary, text("bookings"), String.valueOf(bookings.size()));
        addSummaryCell(summary, text("cancelledBookings"), String.valueOf(cancelled));
        addSummaryCell(summary, text("guests"), String.valueOf(guests));
        addSummaryCell(summary, text("nights"), String.valueOf(nights));
        addSummaryCell(summary, text("charge"), formatMoney(sum(bookable, BookingReportView::charge)));
        addSummaryCell(summary, text("received"), formatMoney(sum(bookable, BookingReportView::received)));
        addSummaryCell(summary, text("rentalTax"), formatMoney(sum(bookable, BookingReportView::rentalTax)));
        addSummaryCell(summary, text("balance"), formatMoney(sum(bookable, BookingReportView::balance)));
        document.add(summary);

        Font noteFont = reportFont(8);
        Paragraph note = new Paragraph(text("activePaid") + ": " + bookable.size()
              + "    " + text("cancelledExcluded"), noteFont);
        note.setSpacingAfter(10);
        document.add(note);
    }

    private void addResourceSummary(Document document) throws DocumentException {
        Paragraph heading = new Paragraph(text("byResource"), reportFont(10));
        heading.setSpacingAfter(4);
        document.add(heading);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {22, 10, 9, 9, 13, 12, 12, 13});
        addHeader(table, List.of(
              text(resourceType), text("bookings"), text("guests"), text("nights"),
              text("charge"), text("received"), text("rentalTax"), text("balance")));

        for (BookingResource resource : resources) {
            List<BookingReportView> rows = bookings.stream()
                  .filter(this::isBookable)
                  .filter(booking -> resource.getId().equals(booking.resourceId()))
                  .toList();
            addCell(table, resource.getName(), reportFont(8), Element.ALIGN_LEFT);
            addCell(table, String.valueOf(rows.size()), reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, String.valueOf(sumInt(rows, BookingReportView::numOfGuests)),
                  reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, String.valueOf(sumInt(rows, BookingReportView::numOfNights)),
                  reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(sum(rows, BookingReportView::charge)), reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(sum(rows, BookingReportView::received)), reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(sum(rows, BookingReportView::rentalTax)), reportFont(8), Element.ALIGN_RIGHT);
            addCell(table, formatMoney(sum(rows, BookingReportView::balance)), reportFont(8), Element.ALIGN_RIGHT);
        }
        table.setHeaderRows(1);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addBookings(Document document) throws DocumentException {
        Paragraph heading = new Paragraph(text("bookingsInPeriod"), reportFont(10));
        heading.setSpacingAfter(4);
        document.add(heading);

        PdfPTable table = new PdfPTable(BOOKING_HEADERS.size());
        table.setWidthPercentage(100);
        table.setWidths(new float[] {7, 8, 8, 10, 12, 10, 5, 5, 7, 7, 7, 7, 7});
        addHeader(table, BOOKING_HEADERS.stream()
              .map(key -> "resource".equals(key) ? text(resourceType) : text(key))
              .toList());
        table.setHeaderRows(1);

        if (bookings.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase(text("noBookings"), reportFont(8)));
            empty.setColspan(BOOKING_HEADERS.size());
            empty.setPadding(5);
            table.addCell(empty);
        } else {
            Font font = reportFont(7);
            for (BookingReportView booking : bookings) {
                addCell(table, status(booking.status()), font, Element.ALIGN_LEFT);
                addCell(table, formatBookingDate(booking.startDate()), font, Element.ALIGN_LEFT);
                addCell(table, formatBookingDate(booking.endDate()), font, Element.ALIGN_LEFT);
                addCell(table, booking.resourceName(), font, Element.ALIGN_LEFT);
                addCell(table, booking.name(), font, Element.ALIGN_LEFT);
                addCell(table, agency(booking.agency()), font, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(Objects.requireNonNullElse(booking.numOfGuests(), 0)),
                      font, Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(Objects.requireNonNullElse(booking.numOfNights(), 0)),
                      font, Element.ALIGN_RIGHT);
                double nights = Objects.requireNonNullElse(booking.numOfNights(), 0);
                double nightlyRate = nights > 0 ? number(booking.rentalTax()) / nights : 0.0;
                addCell(table, formatMoney(nightlyRate), font, Element.ALIGN_RIGHT);
                addCell(table, formatMoney(number(booking.charge())), font, Element.ALIGN_RIGHT);
                addCell(table, formatMoney(number(booking.received())), font, Element.ALIGN_RIGHT);
                addCell(table, formatMoney(number(booking.rentalTax())), font, Element.ALIGN_RIGHT);
                addCell(table, formatMoney(number(booking.balance())), font, Element.ALIGN_RIGHT);
            }
        }
        document.add(table);
    }

    private void addHeader(PdfPTable table, List<String> labels) {
        Font font = reportFont(8);
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, font));
            cell.setBackgroundColor(new Color(235, 239, 245));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private void addSummaryCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Paragraph(label, reportFont(7)));
        cell.addElement(new Paragraph(value, reportFont(9)));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value);
    }

    private String formatBookingDate(java.time.LocalDate value) {
        return value == null ? "" : dateFormatter.format(value);
    }

    private String status(String value) {
        return value == null ? "" : switch (value) {
            case "active", "cancelled", "paid" -> text("status." + value);
            default -> value;
        };
    }

    private String agency(String value) {
        if (value == null) {
            return "";
        }
        String key = switch (resourceType + ":" + value) {
            case "rooms:abnb" -> "agency.rooms.abnb";
            case "rooms:bookingcom" -> "agency.rooms.bookingcom";
            case "rooms:walkin" -> "agency.rooms.walkin";
            case "rooms:closed" -> "agency.rooms.closed";
            case "cars:other" -> "agency.cars.other";
            case "cars:abnb" -> "agency.cars.abnb";
            case "cars:bookingcom" -> "agency.cars.bookingcom";
            case "cars:walkin" -> "agency.cars.walkin";
            case "cars:closed" -> "agency.cars.closed";
            default -> null;
        };
        return key == null ? value : text(key);
    }

    private String text(String key) {
        return ReportMessages.text(locale, key.startsWith("status.") ? key : "report." + key);
    }

    private boolean isBookable(BookingReportView booking) {
        return BookingStatus.isBookable(booking.status());
    }

    private static double sum(List<BookingReportView> rows, Function<BookingReportView, java.math.BigDecimal> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).mapToDouble(java.math.BigDecimal::doubleValue).sum();
    }

    private static double number(java.math.BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private static int sumInt(List<BookingReportView> rows, Function<BookingReportView, Integer> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private static String datePattern(Locale locale) {
        return switch (locale.getLanguage()) {
            case "de", "da" -> "dd.MM.yyyy";
            case "nl" -> "dd-MM-yyyy";
            default -> "dd/MM/yyyy";
        };
    }

    private Font reportFont(float size) {
        return new Font(reportBaseFont, size, Font.NORMAL, Color.BLACK);
    }

    private static BaseFont createReportBaseFont() throws DocumentException, IOException {
        try (InputStream fontStream = BookingsPDFExporter.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (fontStream == null) {
                throw new IOException("Bundled PDF font not found: /fonts/DejaVuSans.ttf");
            }
            return BaseFont.createFont(
                  "DejaVuSans.ttf",
                  BaseFont.IDENTITY_H,
                  BaseFont.EMBEDDED,
                  true,
                  fontStream.readAllBytes(),
                  null);
        }
    }
}
