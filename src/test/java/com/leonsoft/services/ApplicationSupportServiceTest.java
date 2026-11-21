package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;

class ApplicationSupportServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void diagnosticsIncludeVersionAndSafeSettingsButRedactSensitiveLogData() throws Exception {
        Path log = temporaryDirectory.resolve("application.log");
        Files.writeString(log, """
              12:00:00 INFO app.Request - password=top-secret token=abc123 guest@example.com 192.168.1.25
              12:00:01 INFO app.Request - GET /api/v1/booking/report?name=Alice
              """);
        MockEnvironment environment = new MockEnvironment()
              .withProperty("app.deployment-type", "desktop")
              .withProperty("server.address", "127.0.0.1")
              .withProperty("server.port", "8080")
              .withProperty("booking.availability.rooms.prevent-overlap", "true");
        environment.setActiveProfiles("prod");

        String report = new ApplicationSupportService(
              buildProperties(), environment, log.toString()).diagnostics();

        assertTrue(report.contains("Version: 1.2.3"));
        assertTrue(report.contains("Deployment: desktop"));
        assertTrue(report.contains("Active profiles: prod"));
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
              buildProperties(), new MockEnvironment(), "").diagnostics();

        assertTrue(report.contains("Recent logs"));
        assertTrue(report.contains("Not configured for this installation."));
    }

    private static BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.2.3");
        return new BuildProperties(properties);
    }
}
