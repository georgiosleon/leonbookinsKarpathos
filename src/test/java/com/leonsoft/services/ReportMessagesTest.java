package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReportMessagesTest {

    @Test
    void mapsEverySupportedLanguageToItsReportBundle() {
        assertEquals(Locale.UK, ReportMessages.supportedLocale("en"));
        assertEquals(Locale.forLanguageTag("el-GR"), ReportMessages.supportedLocale("el"));
        assertEquals(Locale.GERMANY, ReportMessages.supportedLocale("de"));
        assertEquals(Locale.forLanguageTag("nl-NL"), ReportMessages.supportedLocale("nl"));
        assertEquals(Locale.forLanguageTag("da-DK"), ReportMessages.supportedLocale("da"));
        assertEquals(Locale.FRANCE, ReportMessages.supportedLocale("fr"));
        assertEquals(Locale.UK, ReportMessages.supportedLocale("unsupported"));
        assertEquals(Locale.UK, ReportMessages.supportedLocale(null));

        for (String language : new String[] {"el", "de", "nl", "da", "fr"}) {
            assertNotEquals(
                    ReportMessages.text(Locale.UK, "report.from"),
                    ReportMessages.text(Locale.forLanguageTag(language), "report.from"));
        }
    }
}
