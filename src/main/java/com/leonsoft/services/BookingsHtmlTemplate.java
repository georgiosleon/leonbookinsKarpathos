package com.leonsoft.services;

import com.leonsoft.models.Booking;
import com.leonsoft.models.BookingTotals;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BookingsHtmlTemplate {

    public static String report(String fromDateEuroFmt, String toDateEuroFmt, List<Booking> all) {
        return report(fromDateEuroFmt, toDateEuroFmt, all, Locale.ENGLISH);
    }

    public static String report(
          String fromDateEuroFmt, String toDateEuroFmt, List<Booking> all, Locale locale) {
        log.debug("/booking/report fromDate={} toDate={}", fromDateEuroFmt, toDateEuroFmt);

        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        report
              .append("<h3>").append("  ").append(htmlEscape(ReportMessages.text(locale, "report.from")))
              .append(":  ").append(htmlEscape(fromDateEuroFmt))
              .append("  ").append(htmlEscape(ReportMessages.text(locale, "report.to")))
              .append(":  ").append(htmlEscape(toDateEuroFmt))
              .append("</h3>")

           .append("<table>")

            ///   build row
              .append("<tr>")
              .append("<th>").append(label(locale, "status")).append("</th>")
              .append("<th>").append(label(locale, "arrival")).append("</th>")
              .append("<th>").append(label(locale, "departure")).append("</th>")
              .append("<th>").append(label(locale, "room")).append("</th>")
              .append("<th>").append(label(locale, "name")).append("</th>")
              .append("<th>").append(label(locale, "agency")).append("</th>")
              .append("<th>").append(label(locale, "guests")).append("</th>")
              .append("<th>").append(label(locale, "nights")).append("</th>")
              .append("<th>").append(label(locale, "total")).append("</th>")
              .append("<th>").append(label(locale, "commission")).append("</th>")
              .append("<th>").append(label(locale, "received")).append("</th>")
              .append("<th>").append(label(locale, "balance")).append("</th>")
              .append("<th>").append(label(locale, "country")).append("</th>")
              .append("<th>").append(label(locale, "id")).append("</th>")
              .append("<th>").append(label(locale, "taxNumber")).append("</th>")
              .append("<th>").append(label(locale, "voucher")).append("</th>")
              .append("<th>").append(label(locale, "taxReference")).append("</th>")
              .append("<th>").append(label(locale, "invoice")).append("</th>")
              .append("<th>").append(label(locale, "email")).append("</th>")
              .append("<th>").append(label(locale, "telephone")).append("</th>")
              .append("<th>").append(label(locale, "extraInfo")).append("</th>")

              .append(" </tr>");


        BookingTotals  totals  =  new BookingTotals();

        for (Booking booking : all) {


            ////// pre processing
            LocalDate stDt = BookingsUtils.parseStoredDate(booking.getStartDate());
            LocalDate edDt = BookingsUtils.parseStoredDate(booking.getEndDate());

            String statusClass = Objects.equals(booking.getStatus(), "cancelled")
                  ? " class=\"cancelled-status\""
                  : "";

            /////Calc totals
            totals.setTotBalance(totals.getTotBalance() + booking.getBalance()  );
            totals.setTotCharge(totals.getTotCharge() + booking.getCharge()  );
            totals.setTotCommission(totals.getTotCommission() + booking.getCommission()  );
            totals.setTotReceived(totals.getTotReceived()+ booking.getReceived()  );
            totals.setTotGuests( totals.getTotGuests() + booking.getNumOfGuests()  );
            totals.setTotNights( totals.getTotNights() + booking.getNumOfNights()  );

            // build a row
            report.append("<tr>")

                  .append("<td").append(statusClass).append(">")
                  .append(htmlEscape(status(locale, booking.getStatus()))).append("</td>")

                  .append("<td>").append(dateFormatter.format(stDt)).append("</td>")
                  .append("<td>").append(dateFormatter.format(edDt)).append("</td>")

                  .append("<td>").append(htmlEscape(
                        booking.getRoomName() == null ? booking.getRoom() : booking.getRoomName()))
                  .append("</td>")
                  .append("<td>").append(htmlEscape(booking.getName())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getAgency())).append("</td>")

                  .append("<td>").append(booking.getNumOfGuests()).append("</td>")
                  .append("<td>").append(booking.getNumOfNights()).append("</td>")

                  .append("<td>").append(booking.getCharge() == null ? "0.0" : booking.getCharge()).append("</td>")
                  .append("<td>").append(booking.getCommission() == null ? "0.0" : booking.getCommission()).append("</td>")
                  .append("<td>").append(booking.getReceived() == null ? "0.0" : booking.getReceived()).append("</td>")
                  .append("<td>").append(booking.getBalance() == null ? "0.0" : booking.getBalance()).append("</td>")

                  .append("<td>").append(htmlEscape(booking.getCountry())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.getIdentification())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getAfm())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.getVoucher())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getTaxRefNumber())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getInvoiceNumber())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.getEmail())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getTel())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.getExtraInfo())).append("</td>")

                  .append(" </tr>");

        }

        report.append("  </table>")


        // totals  Table
         .append("<br>")
         .append("<h3>").append(label(locale, "totals")).append("</h3>")
              .append("<table>")
              ///   build row
              .append("<tr>")
              .append("<th>").append(label(locale, "totalGuests")).append("</th>")
              .append("<th>").append(label(locale, "totalNights")).append("</th>")
              .append("<th>").append(label(locale, "totalCharge")).append("</th>")
              .append("<th>").append(label(locale, "totalCommission")).append("</th>")
              .append("<th>").append(label(locale, "totalReceived")).append("</th>")
              .append("<th>").append(label(locale, "totalBalance")).append("</th>")
              .append(" </tr>");

              // build row
              report.append("<tr>")
                  .append("<td>").append( totals.getTotGuests()).append("</td>")
                  .append("<td>").append( totals.getTotNights()).append("</td>")
                  .append("<td>").append(numberFormat.format(totals.getTotCharge())).append("</td>")
                  .append("<td>").append(numberFormat.format(totals.getTotCommission())).append("</td>")
                  .append("<td>").append(numberFormat.format(totals.getTotReceived())).append("</td>")
                  .append("<td>").append(numberFormat.format(totals.getTotBalance())).append("</td>")
              .append(" </tr>");
        report.append("  </table>")

        ;


        return report.toString();

    }

    private static String label(Locale locale, String key) {
        return htmlEscape(ReportMessages.text(locale, "report." + key));
    }

    private static String status(Locale locale, String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "active", "cancelled", "paid" ->
                  ReportMessages.text(locale, "status." + status);
            default -> status;
        };
    }

    private static String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;")
              .replace("\"", "&quot;")
              .replace("'", "&#39;");
    }

}
