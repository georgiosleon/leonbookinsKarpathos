package com.leonsoft.services;


import com.leonsoft.businesslogic.pojos.Booking;
import com.leonsoft.database.repo.BookingRepository;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingsService {

    @Autowired
    protected BookingRepository bookingRepository;

    public DateTimeFormatter formatterEURO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public DateTimeFormatter formatterISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void exportToPDF(HttpServletResponse response, String fromDateEuroFmt, String toDateEuroFmt
    ) throws DocumentException, IOException {

        log.info("/booking/export/pdf");

        log.info("fromDate param  " + fromDateEuroFmt);
        log.info("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, formatterEURO);

        List<Booking> results = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));

        response.setContentType("application/pdf");

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=bookings_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        BookingsPDFExporter exporter = new BookingsPDFExporter(results);
        exporter.export(response);

    }

    public String report(String fromDateEuroFmt, String toDateEuroFmt) {

        log.info("/booking/report");

        log.info("fromDate param  " + fromDateEuroFmt);
        log.info("toDate param  " + toDateEuroFmt);

        LocalDate stDate = LocalDate.parse(fromDateEuroFmt, formatterEURO);
        LocalDate edDate = LocalDate.parse(toDateEuroFmt, formatterEURO);

        List<Booking> all = bookingRepository.findByStartDateBetweenOrderByStartDateDesc(
              stDate.format(DateTimeFormatter.ISO_DATE),
              edDate.format(DateTimeFormatter.ISO_DATE));

        StringBuilder report = new StringBuilder();

        String pdfUrl = "/booking/export/pdf?fromDate=" + fromDateEuroFmt + "&toDate=" + toDateEuroFmt;

        report.append("<h3>")
              .append("  From:  ").append(fromDateEuroFmt).append("  To:  ").append(toDateEuroFmt)
              .append("        <a href=\"" + pdfUrl + "\">Export to PDF</a>  ")
              .append("</h3>")

              .append("<table>")
              .append("<tr>")

              .append("<th>Status</th>")
              .append("<th>Arrival</th>")
              .append("<th>Departure</th>")
              .append("<th>Room </th>")
              .append("<th>Name </th>")
              .append("<th>Agency </th>")

              .append("<th>Guests </th>")
              .append("<th>Nights </th>")

              .append("<th>Total </th>")
              .append("<th>Commission </th>")
              .append("<th>Received </th>")
              .append("<th>Balance </th>")

              .append("<th>Country </th>")

              .append("<th>ID </th>")
              .append("<th>ΑΦΜ</th>")

              .append("<th>Voucher </th>")
              .append("<th>Tax Ref. Number </th>")
              .append("<th>Invoice Number </th>")

              .append("<th>Email</th>")
              .append("<th>Tel </th>")
              .append("<th>Extra Info</th>")

              .append(" </tr>");

        for (Booking booking : all) {

            LocalDate stDt = LocalDate.parse(booking.getStartDate(), formatterISO);
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), formatterISO);

            String delUrl = "/booking/del?bid=" + booking.getId();

            String statusCancelled = "<td     style=\"    background-color: rgb(247, 201, 201);   color: rgb(27, 6, 46);  \">" +

                  "<a href='" + delUrl + "'  target='_self'   >" + booking.getStatus() + "</a>"
//                  booking.getStatus()

                  + "</td>";
            String statusActive = "<td>" + booking.getStatus() + "</td>";
            String status = statusActive;
            if (Objects.equals(booking.getStatus(), "cancelled")) {
                status = statusCancelled;
            }

            report.append("<tr>")

                  .append(status)

                  .append("<td>").append(formatterEURO.format(stDt)).append("</td>")
                  .append("<td>").append(formatterEURO.format(edDt)).append("</td>")

                  .append("<td>").append(booking.getRoom()).append("</td>")
                  .append("<td>").append(booking.getName()).append("</td>")
                  .append("<td>").append(booking.getAgency()).append("</td>")

                  .append("<td>").append(booking.getNumOfGuests()).append("</td>")
                  .append("<td>").append(booking.getNumOfNights()).append("</td>")

                  .append("<td>").append(booking.getCharge() == null ? "0.0" : booking.getCharge()).append("</td>")
                  .append("<td>").append(booking.getCommission() == null ? "0.0" : booking.getCommission()).append("</td>")
                  .append("<td>").append(booking.getReceived() == null ? "0.0" : booking.getReceived()).append("</td>")
                  .append("<td>").append(booking.getBalance() == null ? "0.0" : booking.getBalance()).append("</td>")

                  .append("<td>").append(booking.getCountry() == null ? "" : booking.getCountry()).append("</td>")

                  .append("<td>").append(booking.getIdentification() == null ? "" : booking.getIdentification()).append("</td>")
                  .append("<td>").append(booking.getAfm() == null ? "" : booking.getAfm()).append("</td>")

                  .append("<td>").append(booking.getVoucher() == null ? "" : booking.getVoucher()).append("</td>")
                  .append("<td>").append(booking.getTaxRefNumber() == null ? "" : booking.getTaxRefNumber()).append("</td>")
                  .append("<td>").append(booking.getInvoiceNumber() == null ? "" : booking.getInvoiceNumber()).append("</td>")

                  .append("<td>").append(booking.getEmail() == null ? "" : booking.getEmail()).append("</td>")
                  .append("<td>").append(booking.getTel() == null ? "" : booking.getTel()).append("</td>")
                  .append("<td>").append(booking.getExtraInfo() == null ? "" : booking.getExtraInfo()).append("</td>")

                  .append(" </tr>");

        }

        report.append("  </table>");

        return report.toString();

    }

    public Booking create(Booking booking) {

        booking.setId(UUID.randomUUID().toString());
        // todo need to change all ids  to new way  :)
        //booking.setId(""+booking.hashCode() );

        log.info(" SAVE   {}  ", booking);

        // convert from String to localDate
        LocalDate stDate = LocalDate.parse(booking.getStartDate(), formatterEURO);
        LocalDate edDate = LocalDate.parse(booking.getEndDate(), formatterEURO);

        // save  ISO String formate
        booking.setStartDate(stDate.format(DateTimeFormatter.ISO_DATE));
        booking.setEndDate(edDate.format(DateTimeFormatter.ISO_DATE));

        booking.setCharge(booking.getCharge() == null ? 0 : booking.getCharge());
        booking.setCommission(booking.getCommission() == null ? 0.0 : booking.getCommission());
        booking.setReceived(booking.getReceived() == null ? 0.0 : booking.getReceived());
        booking.setBalance(booking.getBalance() == null ? 0.0 : booking.getBalance());

        booking = bookingRepository.save(booking);

        // send back EURO format
        booking.setStartDate(formatterEURO.format(stDate));
        booking.setEndDate(formatterEURO.format(edDate));

        return booking;
    }

    public Boolean deleteObject(Booking booking) {
        log.info(" Delete    {}", booking.getId());
        bookingRepository.deleteById(booking.getId());
        return true;
    }

    public void delCancelledBooking(String bookingId) {
        log.info(" Delete  a cancelled   {}", bookingId);
        bookingRepository.deleteById(bookingId);
    }

    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAllByOrderByStartDateAsc();
        for (Booking booking : all) {
            LocalDate stDt = LocalDate.parse(booking.getStartDate(), formatterISO);
            booking.setStartDate(formatterEURO.format(stDt));
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), formatterISO);
            booking.setEndDate(formatterEURO.format(edDt));
        }
        return all;
    }

}
