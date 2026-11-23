package com.leonsoft.services;

import com.leonsoft.models.BookingStatus;
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

    public static String report(String fromDateEuroFmt, String toDateEuroFmt, List<BookingReportView> all) {
        return report(fromDateEuroFmt, toDateEuroFmt, all, Locale.ENGLISH);
    }

    public static String report(
          String fromDateEuroFmt, String toDateEuroFmt, List<BookingReportView> all, Locale locale) {
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

        for (BookingReportView booking : all) {


            ////// pre processing
            LocalDate stDt = BookingsUtils.parseStoredDate(booking.startDate());
            LocalDate edDt = BookingsUtils.parseStoredDate(booking.endDate());

            String statusClass = BookingStatus.isCancelled(booking.status())
                  ? " class=\"cancelled-status\""
                  : "";

            /////Calc totals
            totals.setTotBalance(totals.getTotBalance() + n(booking.balance()));
            totals.setTotCharge(totals.getTotCharge() + n(booking.charge()));
            totals.setTotCommission(totals.getTotCommission() + n(booking.commission()));
            totals.setTotReceived(totals.getTotReceived() + n(booking.received()));
            totals.setTotGuests(totals.getTotGuests() + n(booking.numOfGuests()));
            totals.setTotNights(totals.getTotNights() + n(booking.numOfNights()));

            // build a row
            report.append("<tr>")

                  .append("<td").append(statusClass).append(">")
                  .append(htmlEscape(status(locale, booking.status()))).append("</td>")

                  .append("<td>").append(dateFormatter.format(stDt)).append("</td>")
                  .append("<td>").append(dateFormatter.format(edDt)).append("</td>")

                  .append("<td>").append(htmlEscape(booking.roomName()))
                  .append("</td>")
                  .append("<td>").append(htmlEscape(booking.name())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.agency())).append("</td>")

                  .append("<td>").append(n(booking.numOfGuests())).append("</td>")
                  .append("<td>").append(n(booking.numOfNights())).append("</td>")

                  .append("<td>").append(n(booking.charge())).append("</td>")
                  .append("<td>").append(n(booking.commission())).append("</td>")
                  .append("<td>").append(n(booking.received())).append("</td>")
                  .append("<td>").append(n(booking.balance())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.country())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.identification())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.afm())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.voucher())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.taxRefNumber())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.invoiceNumber())).append("</td>")

                  .append("<td>").append(htmlEscape(booking.email())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.tel())).append("</td>")
                  .append("<td>").append(htmlEscape(booking.extraInfo())).append("</td>")

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



    private static double n(Double value) {
        return value == null ? 0.0 : value;
    }

    private static int n(Integer value) {
        return value == null ? 0 : value;
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
