package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class DatabaseSchemaTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void initializesBookingAndResourceTablesAndPreservesExistingBookings() throws Exception {
        Path database = temporaryDirectory.resolve("bookings.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("demo-data.sql"));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate(
                        "INSERT INTO Booking (id, name) VALUES ('existing-booking', 'Guest')");
            }

            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));

            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT name FROM Booking WHERE id = 'existing-booking'")) {
                result.next();
                assertEquals("Guest", result.getString("name"));
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM Booking WHERE id = 'DEMO-BOOKING-1'")) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM booking_resources")) {
                result.next();
                assertEquals(25, result.getInt(1));
            }
        }
    }

    @Test
    void initializesNewCatalogOnLegacyDatabaseWithoutChangingExistingBookings() throws Exception {
        Path database = temporaryDirectory.resolve("legacy-bookings.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/legacy-bookings.sql"));

            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("demo-data.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("demo-data.sql"));

            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT name, room, start_date, end_date, status FROM Booking "
                               + "WHERE id = 'legacy-booking-1'")) {
                result.next();
                assertEquals("Existing Customer", result.getString("name"));
                assertEquals("APT1", result.getString("room"));
                assertEquals("2025-07-10", result.getString("start_date"));
                assertEquals("2025-07-14", result.getString("end_date"));
                assertEquals("active", result.getString("status"));
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM Booking WHERE id = 'DEMO-BOOKING-1'")) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM booking_resources")) {
                result.next();
                assertEquals(25, result.getInt(1));
            }
        }
    }
}
