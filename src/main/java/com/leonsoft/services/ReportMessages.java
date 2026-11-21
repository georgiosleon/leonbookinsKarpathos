package com.leonsoft.services;

import java.util.Locale;
import java.util.ResourceBundle;

public final class ReportMessages {
    private ReportMessages() {
    }

    public static Locale supportedLocale(String language) {
        if (language == null) {
            return Locale.UK;
        }
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "el", "el-gr" -> Locale.forLanguageTag("el-GR");
            case "de", "de-de" -> Locale.GERMANY;
            case "nl", "nl-nl" -> Locale.forLanguageTag("nl-NL");
            case "da", "da-dk" -> Locale.forLanguageTag("da-DK");
            case "fr", "fr-fr" -> Locale.FRANCE;
            default -> Locale.UK;
        };
    }

    public static String text(Locale locale, String key) {
        Locale supported = supportedLocale(locale == null ? null : locale.getLanguage());
        return ResourceBundle.getBundle("reports", supported).getString(key);
    }
}
