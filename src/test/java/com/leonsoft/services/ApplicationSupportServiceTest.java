package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.leonsoft.api.LicenseStatusResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;

class ApplicationSupportServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void diagnosticsIncludeBuildSchemaLicenseAndSafeSettingsButRedactSensitiveLogData() throws Exception {
        Path log = temporaryDirectory.resolve("application.log");
        Files.writeString(log, """
               12:00:00 INFO app.Request - password=top-secret token=abc123 guest@example.com 192.168.1.25
               12:00:01 INFO app.Request - GET /api/v1/booking/report?name=Alice
               """);
        MockEnvironment environment = new MockEnvironment()
              .withProperty("app.build.id", "desktop-test-build")
              .withProperty("app.deployment-type", "desktop")
              .withProperty("server.address", "127.0.0.1")
              .withProperty("server.port", "8080")
              .withProperty("booking.availability.rooms.prevent-overlap", "true");
        environment.setActiveProfiles("prod");

        String report = new ApplicationSupportService(
              buildProperties(), environment, log.toString(), licenseService(true), flyway()).diagnostics();

        assertTrue(report.contains("Version: 1.2.3"));
        assertTrue(report.contains("Build ID: desktop-test-build"));
        assertTrue(report.contains("Deployment: desktop"));
        assertTrue(report.contains("Active profiles: prod"));
        assertTrue(report.contains("Schema version: 12"));
        assertTrue(report.contains("OS: " + System.getProperty("os.name")));
        assertTrue(report.contains("License state: ACTIVE"));
        assertTrue(report.contains("Install ID: install-1"));
        assertTrue(report.contains("Redaction: log lines are masked"));
        assertTrue(report.contains("Database: SQLite (database path omitted)"));
        assertTrue(report.contains("password=[REDACTED]"));
        assertTrue(report.contains("token=[REDACTED]"));
        assertTrue(report.contains("[EMAIL]"));
        assertTrue(report.contains("[IP]"));
        assertTrue(report.contains("name=[REDACTED]"));
        assertFalse(report.contains("top-secret"));
        assertFalse(report.contains("abc123"));
        assertFalse(report.contains("guest@example.com"));
        assertFalse(report.contains("192.168.1.25"));
        assertFalse(report.contains("Alice"));
        assertFalse(report.contains(temporaryDirectory.toString()));
    }

    @Test
    void diagnosticsClearlyIndicateWhenLogCaptureIsNotConfigured() {
        String report = new ApplicationSupportService(
              buildProperties(), new MockEnvironment(), "", licenseService(false), flyway()).diagnostics();

        assertTrue(report.contains("Build ID: build-1.2.3"));
        assertTrue(report.contains("License state: NOT ACTIVATED"));
        assertTrue(report.contains("Recent logs"));
        assertTrue(report.contains("Not configured for this installation."));
    }

    @Test
    void supportBundleIncludesDiagnosticsAndRicherAppInfo() throws Exception {
        MockEnvironment environment = new MockEnvironment()
              .withProperty("app.build.id", "desktop-test-build")
              .withProperty("app.deployment-type", "desktop");
        ApplicationSupportService service = new ApplicationSupportService(
              buildProperties(), environment, "", licenseService(true), flyway());

        byte[] bundle = service.supportBundle();
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bundle))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        assertTrue(entries.containsKey("diagnostics.txt"));
        assertTrue(entries.get("diagnostics.txt").contains("Build ID: desktop-test-build"));
        String json = entries.get("app-info.json");
        assertTrue(json.contains("\"version\": \"1.2.3\""));
        assertTrue(json.contains("\"buildId\": \"desktop-test-build\""));
        assertTrue(json.contains("\"schemaVersion\": \"12\""));
        assertTrue(json.contains("\"licenseState\": \"ACTIVE\""));
        assertTrue(json.contains("\"licenseActivated\": true"));
    }

    private static BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.2.3");
        properties.setProperty("buildId", "build-1.2.3");
        return new BuildProperties(properties);
    }

    private static LicenseService licenseService(boolean activated) {
        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.getStatus()).thenReturn(
              new LicenseStatusResponse(activated, "install-1", null, null, null));
        return licenseService;
    }

    private static Flyway flyway() {
        Flyway flyway = mock(Flyway.class);
        MigrationInfoService infoService = mock(MigrationInfoService.class);
        MigrationInfo current = mock(MigrationInfo.class);
        when(flyway.info()).thenReturn(infoService);
        when(infoService.current()).thenReturn(current);
        when(current.getVersion()).thenReturn(MigrationVersion.fromVersion("12"));
        return flyway;
    }
}
