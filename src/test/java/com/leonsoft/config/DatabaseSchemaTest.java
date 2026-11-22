package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class DatabaseSchemaTest {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Path database = temporaryDirectory.resolve("schema-test.db");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database.toAbsolutePath());
        registry.add("APP_DB_URL", () -> "jdbc:sqlite:" + database.toAbsolutePath());
        registry.add("url", () -> "jdbc:sqlite:" + database.toAbsolutePath());
    }

    @Test
    void initializesExpectedTables() throws Exception {
        Path database = temporaryDirectory.resolve("schema-test.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath())) {
            assertTrue(tableExists(connection, "Booking"));
            assertTrue(tableExists(connection, "booking_resources"));
            assertTrue(tableExists(connection, "users"));
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
