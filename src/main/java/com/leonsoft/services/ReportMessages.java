package com.leonsoft.services;

import java.util.Locale;
import java.util.ResourceBundle;

public final class ReportMessages {
    private ReportMessages() {
    }

    public static Locale supportedLocale(String language) {
        return "el".equalsIgnoreCase(language) ? Locale.forLanguageTag("el-GR") : Locale.UK;
    }

    public static String text(Locale locale, String key) {
        Locale supported = "el".equals(locale.getLanguage()) ? Locale.forLanguageTag("el-GR") : Locale.UK;
        return ResourceBundle.getBundle("reports", supported).getString(key);
    }
}
