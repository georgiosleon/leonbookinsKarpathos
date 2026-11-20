package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

public class BookingsPDFExporter {
    private static final List<String> HEADER_KEYS = List.of(
            "status", "arrival", "departure", "room", "name", "agency", "guests", "nights",
            "total", "commission", "received", "balance", "country", "id", "taxNumber",
            "voucher", "invoice", "taxReference");

    private final List<Booking> bookingList;
    private final Locale locale;

    public BookingsPDFExporter(List<Booking> bookingList, Locale locale) {
        this.bookingList = bookingList;
        this.locale = locale;
    }

    private void writeTableHeader(PdfPTable table) {
        Font font = reportFont(8);
        for (String key : HEADER_KEYS) {
            PdfPCell cell = new PdfPCell(new Phrase(ReportMessages.text(locale, "report." + key), font));
            cell.setBackgroundColor(Color.WHITE);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private void writeTableData(PdfPTable table) {
        Font font = reportFont(7);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        for (Booking booking : bookingList) {
            addCell(table, translatedStatus(booking.getStatus()), font);
            addCell(table, formatDate(booking.getStartDate(), dateFormatter), font);
            addCell(table, formatDate(booking.getEndDate(), dateFormatter), font);
            addCell(table, booking.getRoomName() == null ? booking.getRoom() : booking.getRoomName(), font);
            addCell(table, booking.getName(), font);
            addCell(table, booking.getAgency(), font);
            addCell(table, String.valueOf(booking.getNumOfGuests()), font);
            addCell(table, String.valueOf(booking.getNumOfNights()), font);
            addCell(table, formatNumber(booking.getCharge(), numberFormat), font);
            addCell(table, formatNumber(booking.getCommission(), numberFormat), font);
            addCell(table, formatNumber(booking.getReceived(), numberFormat), font);
            addCell(table, formatNumber(booking.getBalance(), numberFormat), font);
            addCell(table, booking.getCountry(), font);
            addCell(table, booking.getIdentification(), font);
            addCell(table, booking.getAfm(), font);
            addCell(table, booking.getVoucher(), font);
            addCell(table, booking.getInvoiceNumber(), font);
            addCell(table, booking.getTaxRefNumber(), font);
        }
    }

    public void export(HttpServletResponse response) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate());
        document.setMargins(6, 6, 20, 20);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        PdfPTable table = new PdfPTable(HEADER_KEYS.size());
        table.setWidthPercentage(100f);
        table.setSpacingBefore(1);
        writeTableHeader(table);
        writeTableData(table);
        document.add(table);
        document.close();
    }

    private static Font reportFont(float size) {
        Font font = FontFactory.getFont(FontFactory.COURIER, "Cp1253", false);
        font.setSize(size);
        font.setColor(Color.BLACK);
        return font;
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text == null ? "" : text, font)));
    }

    private String translatedStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "active", "cancelled", "paid" ->
                    ReportMessages.text(locale, "status." + status);
            default -> status;
        };
    }

    private static String formatDate(String value, DateTimeFormatter formatter) {
        return value == null ? "" : formatter.format(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private static String formatNumber(Number value, NumberFormat formatter) {
        return formatter.format(value == null ? 0 : value);
    }
}
