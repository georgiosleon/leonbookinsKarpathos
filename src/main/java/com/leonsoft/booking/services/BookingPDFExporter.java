package com.leonsoft.booking.services;

import com.leonsoft.booking.models.Booking;
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
import java.util.List;


public class BookingPDFExporter {

    private final List<Booking> bookingList;

    public BookingPDFExporter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();

        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(3);

        Font font = FontFactory.getFont(FontFactory.COURIER);
        font.setSize(8);
        font.setColor(Color.BLACK);

        cell.setPhrase(new Phrase("Status", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Arrival", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Departure", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Room", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Name", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Agency", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Guests", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Nights", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Total", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Commission", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Received", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Balance", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Country", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("ID", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("AFM", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Voucher", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Invoice", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Tax Ref.", font));
        table.addCell(cell);


    }

    private void writeTableData(PdfPTable table) {

        Font font = FontFactory.getFont(FontFactory.COURIER);
        font.setSize(7);

        for (Booking booking : bookingList) {
            PdfPCell cell = new PdfPCell();

            cell.setPhrase(new Phrase(booking.getStatus(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getStartDate(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getEndDate(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getRoom(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getName(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getAgency(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getNumOfGuests()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getNumOfNights()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getCharge()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getCommission()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getReceived()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(String.valueOf(booking.getBalance()), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getCountry(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getIdentification(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getAfm(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getVoucher(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getInvoiceNumber(), font));
            table.addCell(cell);

            cell.setPhrase(new Phrase(booking.getTaxRefNumber(), font));
            table.addCell(cell);


        }
    }

    public void export(HttpServletResponse response) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate());

        document.setMargins(6, 6, 20, 20);

        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
//        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
//        font.setSize(7);
//        font.setColor(Color.BLUE);

//        Paragraph p = new Paragraph("Bookings", font);
//        p.setAlignment(Paragraph.ALIGN_CENTER);
//
//        document.add(p);

        PdfPTable table = new PdfPTable(18);
        table.setWidthPercentage(100f);

//        table.setWidths(new float[]{1.5f, 3.5f, 3.0f, 3.0f, 1.5f});
        table.setSpacingBefore(1);

        writeTableHeader(table);
        writeTableData(table);

        document.add(table);

        document.close();

    }
}
