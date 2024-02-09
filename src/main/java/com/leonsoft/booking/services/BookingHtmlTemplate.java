package com.leonsoft.booking.services;

import com.leonsoft.booking.models.Booking;
import com.leonsoft.booking.models.BookingTotals;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BookingHtmlTemplate {

    public static String report(String fromDateEuroFmt, String toDateEuroFmt, List<Booking> all) {

        log.debug("/booking/report");
        log.debug("fromDate param  " + fromDateEuroFmt);
        log.debug("toDate param  " + toDateEuroFmt);

        StringBuilder report = new StringBuilder();

        String pdfUrl = "/booking/export/pdf?fromDate=" + fromDateEuroFmt + "&toDate=" + toDateEuroFmt;


        report
              .append("<h3>").append("  From:  ").append(fromDateEuroFmt).append("  To:  ").append(toDateEuroFmt).append("        <a href=\"" + pdfUrl + "\">Export to PDF</a>  ")
              .append("</h3>")

           .append("<table>")

            ///   build row
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


        BookingTotals  totals  =  new BookingTotals();

        for (Booking booking : all) {


            ////// pre processing
            LocalDate stDt = LocalDate.parse(booking.getStartDate(), DateTimeService.formatterISO);
            LocalDate edDt = LocalDate.parse(booking.getEndDate(), DateTimeService.formatterISO);

//            String statusCancelled = "<td     style=\"    background-color: rgb(247, 201, 201);   color: rgb(27, 6, 46);  \">" +
//                  " <a id='delAct' href='#' onclick='delAction(\""+ booking.getId()+"\");return false;'> " + booking.getStatus() + "</a>"
//                  + "</td>";

            String status = "<td>" + booking.getStatus() + "</td>";
//            if (Objects.equals(booking.getStatus(), "cancelled")) {
//                status = statusCancelled;
//            }

            /////Calc totals
            totals.setTotBalance(totals.getTotBalance() + booking.getBalance()  );
            totals.setTotCharge(totals.getTotCharge() + booking.getCharge()  );
            totals.setTotCommission(totals.getTotCommission() + booking.getCommission()  );
            totals.setTotReceived(totals.getTotReceived()+ booking.getReceived()  );
            totals.setTotGuests( totals.getTotGuests() + booking.getNumOfGuests()  );
            totals.setTotNights( totals.getTotNights() + booking.getNumOfNights()  );

            // build a row
            report.append("<tr>")

                  .append(status)

                  .append("<td>").append(DateTimeService.formatterEURO.format(stDt)).append("</td>")
                  .append("<td>").append(DateTimeService.formatterEURO.format(edDt)).append("</td>")

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

        report.append("  </table>")


        // totals  Table
         .append("<br>")
         .append("<h3>").append("TOTALS ").append("</h3>")
              .append("<table>")
              ///   build row
              .append("<tr>")
              .append("<th>Total Guests</th>")
              .append("<th>Total Nights</th>")
              .append("<th>Total Charge</th>")
              .append("<th>Total Commission</th>")
              .append("<th>Total Received</th>")
              .append("<th>Total Balance</th>")
              .append(" </tr>");

              // build row
              report.append("<tr>")
                  .append("<td>").append( totals.getTotGuests()).append("</td>")
                  .append("<td>").append( totals.getTotNights()).append("</td>")
                  .append("<td>").append( DateTimeService.df.format( totals.getTotCharge())).append("</td>")
                  .append("<td>").append(  DateTimeService.df.format(totals.getTotCommission())).append("</td>")
                  .append("<td>").append(  DateTimeService.df.format( totals.getTotReceived())) .append("</td>")
                  .append("<td>").append( DateTimeService.df.format( totals.getTotBalance() )) .append("</td>")
              .append(" </tr>");
        report.append("  </table>")

        ;


        return report.toString();

    }

}
