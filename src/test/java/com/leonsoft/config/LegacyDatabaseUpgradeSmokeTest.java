package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.license.bypass=false")
@AutoConfigureMockMvc
class LegacyDatabaseUpgradeSmokeTest {

    @TempDir
    static Path tempDir;
    static Path db;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws Exception {
        db = tempDir.resolve("legacy-upgrade.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath())) {
            // Simulate a V1-era database already baselined by Flyway.
            String v1;
            try (var in = LegacyDatabaseUpgradeSmokeTest.class.getResourceAsStream("/db/migration/V1__initial_schema.sql")) {
                v1 = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            for (String stmt : v1.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) { try (var st = c.createStatement()) { st.execute(s); } }
            }
            try (var st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS flyway_schema_history (installed_rank INTEGER NOT NULL PRIMARY KEY, version TEXT, description TEXT, type TEXT NOT NULL, script TEXT NOT NULL, checksum INTEGER, installed_by TEXT NOT NULL, installed_on TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now')), execution_time INTEGER NOT NULL, success INTEGER NOT NULL)");
                st.execute("INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'initial schema', 'BASELINE', '<< Flyway Baseline >>', NULL, 'test', '2026-01-01 00:00:00', 0, 1)");
                st.execute("INSERT INTO Booking (id, name, room, start_date, end_date, status) VALUES ('legacy-booking-1', 'Existing Customer', 'APT1', '2025-07-10', '2025-07-14', 'active')");
            }
        }
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        registry.add("spring.datasource.url", () -> url);
        registry.add("APP_DB_URL", () -> url);
        registry.add("url", () -> url);
    }

    @Autowired MockMvc mockMvc;

    @Test
    void legacyDatabaseMigratesAndAppSmokeWorks() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath())) {
            assertTrue(tableExists(c, "flyway_schema_history"));
            assertTrue(tableExists(c, "audit_log"));
            assertTrue(tableExists(c, "app_license"));
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM bookings")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 1);
            }
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'Booking'")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "uppercase Booking table should be renamed by V12");
            }
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '12' AND success = 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "V12 should be recorded as successful");
            }
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "SELECT resource_id, start_date, end_date, status FROM bookings WHERE id = 'legacy-booking-1'")) {
                assertTrue(rs.next());
                assertEquals("APT1", rs.getString("resource_id"));
                assertEquals("2025-07-10", rs.getString("start_date"));
                assertEquals("2025-07-14", rs.getString("end_date"));
                assertEquals("active", rs.getString("status"));
            }
        }

        mockMvc.perform(get("/api/v1/license/status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installId").isNotEmpty());
        // Unactivated legacy-upgraded install should still enforce license gate
        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    private static boolean tableExists(Connection c, String table) throws Exception {
        try (var rs = c.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        }
    }
}
