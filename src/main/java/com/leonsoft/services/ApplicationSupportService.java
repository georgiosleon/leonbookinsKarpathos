package com.leonsoft.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ApplicationSupportService {
    private static final int MAX_LOG_BYTES = 64 * 1024;
    private static final int MAX_LOG_LINES = 200;
    private static final Pattern SECRET = Pattern.compile(
          "(?i)(password|passphrase|secret|token|authorization|cookie|api[_-]?key)"
                + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;]+)");
    private static final Pattern EMAIL = Pattern.compile(
          "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPV4 = Pattern.compile(
          "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern PHONE = Pattern.compile(
          "(?<!\\w)\\+?\\d[\\d ().-]{7,}\\d(?!\\w)");
    private static final Pattern QUERY_VALUE = Pattern.compile("([?&][^\\s=&]+)=([^&\\s]+)");
    private static final Pattern BOOKING_ID = Pattern.compile(
          "(?i)\\b(?:booking|guest|customer)[-_ ]?(?:id)?\\s*[:=#]?\\s*[A-Z0-9-]{4,}");

    private final BuildProperties buildProperties;
    private final Environment environment;
    private final String logFile;
    private final LicenseService licenseService;

    public ApplicationSupportService(
          BuildProperties buildProperties,
          Environment environment,
          @Value("${app.diagnostics.log-file:}") String logFile,
          LicenseService licenseService) {
        this.buildProperties = buildProperties;
        this.environment = environment;
        this.logFile = logFile;
        this.licenseService = licenseService;
    }

    public AppInfo appInfo() {
        var licenseStatus = licenseService.getStatus();
        return new AppInfo(
              buildProperties.getVersion(),
              environment.getProperty("app.deployment-type", "standalone"),
              licenseStatus.activated());
    }

    public String diagnostics() {
        StringBuilder report = new StringBuilder()
              .append("Leon Bookings diagnostics\n")
              .append("Version: ").append(buildProperties.getVersion()).append('\n')
              .append("Deployment: ").append(environment.getProperty("app.deployment-type", "standalone")).append('\n')
              .append("Active profiles: ").append(activeProfiles()).append('\n')
              .append("Java: ").append(System.getProperty("java.version")).append('\n')
              .append("OS: ").append(System.getProperty("os.name"))
              .append(" (").append(System.getProperty("os.arch")).append(")\n")
              .append("Server address: ").append(environment.getProperty("server.address", "default")).append('\n')
              .append("Server port: ").append(environment.getProperty("server.port", "8080")).append('\n')
              .append("Database: SQLite (database path omitted)\n")
              .append("Room overlap prevention: ")
              .append(environment.getProperty("booking.availability.rooms.prevent-overlap", "true")).append('\n')
              .append("Vehicle overlap prevention: ")
              .append(environment.getProperty("booking.availability.cars.prevent-overlap", "true")).append('\n')
              .append("License activated: ").append(licenseService.isActivated()).append('\n')
              .append("\nRecent logs (last ").append(MAX_LOG_LINES).append(" lines maximum):\n");
        appendLogs(report);
        return report.toString();
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "default" : String.join(", ", profiles);
    }

    private void appendLogs(StringBuilder report) {
        if (logFile.isBlank()) {
            report.append("Not configured for this installation.\n");
            return;
        }
        Path path = Path.of(logFile).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
            report.append("Configured log file is unavailable.\n");
            return;
        }
        try {
            report.append(readSanitizedLogTail(path));
        } catch (IOException exception) {
            report.append("Could not read log file (")
                  .append(exception.getClass().getSimpleName()).append(").\n");
        }
    }

    private static String readSanitizedLogTail(Path path) throws IOException {
        long size = Files.size(path);
        long start = Math.max(0, size - MAX_LOG_BYTES);
        try (InputStream input = Files.newInputStream(path)) {
            input.skipNBytes(start);
            byte[] bytes = input.readNBytes(MAX_LOG_BYTES);
            String contents = new String(bytes, StandardCharsets.UTF_8);
            List<String> lines = Arrays.stream(contents.split("\\R"))
                  .filter(line -> !line.isBlank())
                  .toList();
            return lines.stream()
                  .skip(Math.max(0, lines.size() - MAX_LOG_LINES))
                  .map(ApplicationSupportService::sanitizeLogLine)
                  .collect(Collectors.joining("\n", "", "\n"));
        }
    }

    static String sanitizeLogLine(String line) {
        String sanitized = SECRET.matcher(line).replaceAll("$1=[REDACTED]");
        sanitized = EMAIL.matcher(sanitized).replaceAll("[EMAIL]");
        sanitized = IPV4.matcher(sanitized).replaceAll("[IP]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[PHONE]");
        sanitized = QUERY_VALUE.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = BOOKING_ID.matcher(sanitized).replaceAll("[RECORD-ID]");
        return sanitized.replaceAll("[\\p{Cntrl}&&[^\\t]]", "");
    }

    public record AppInfo(String version, String deploymentType, boolean licenseActivated) {
    }
}
