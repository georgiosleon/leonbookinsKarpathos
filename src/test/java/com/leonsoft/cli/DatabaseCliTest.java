package com.leonsoft.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseCliTest {
    @TempDir
    Path temporaryDirectory;

    private Path database;
    private Path source;
    private Path backupDirectory;

    @BeforeEach
    void setUp() throws SQLException {
        database = temporaryDirectory.resolve("bookings.db");
        source = temporaryDirectory.resolve("restore-source.sqlite3");
        backupDirectory = temporaryDirectory.resolve("backups");
        createDatabase(database, "current");
        createDatabase(source, "restored");
    }

    @Test
    void backupCopiesDatabaseWithoutChangingSource() throws Exception {
        Path output = backupDirectory.resolve("manual.sqlite3");

        Path result = DatabaseCli.backupDatabase(database, output);

        assertEquals(output.toAbsolutePath().normalize(), result);
        assertEquals("current", bookingName(output));
        assertEquals("current", bookingName(database));
    }

    @Test
    void backupDoesNotOverwriteExistingFile() throws Exception {
        Path output = temporaryDirectory.resolve("existing.sqlite3");
        Files.writeString(output, "keep this file");

        assertThrows(IllegalArgumentException.class,
                () -> DatabaseCli.backupDatabase(database, output));

        assertEquals("keep this file", Files.readString(output));
    }

    @Test
    void encryptedBackupCanBeRestoredOnlyWithItsPassphrase() throws Exception {
        Path encryptedBackup = backupDirectory.resolve("support.sqlite3.enc");
        char[] passphrase = "correct horse battery".toCharArray();
        char[] wrongPassphrase = "incorrect horse battery".toCharArray();

        try {
            DatabaseCli.backupDatabaseEncrypted(database, encryptedBackup, passphrase);
            updateBookingName(database, "changed");

            assertTrue(EncryptedDatabaseBackup.isEncrypted(encryptedBackup));
            assertFalse(new String(Files.readAllBytes(encryptedBackup), StandardCharsets.US_ASCII)
                    .startsWith("SQLite format 3"));
            assertThrows(java.io.IOException.class, () -> DatabaseCli.restoreEncryptedDatabase(
                    database, encryptedBackup, backupDirectory, wrongPassphrase));
            assertEquals("changed", bookingName(database));

            DatabaseCli.restoreEncryptedDatabase(database, encryptedBackup, backupDirectory, passphrase);
            assertEquals("current", bookingName(database));
        } finally {
            Arrays.fill(passphrase, '\0');
            Arrays.fill(wrongPassphrase, '\0');
        }
    }

    @Test
    void encryptedBackupDoesNotOverwriteExistingFile() throws Exception {
        Path output = temporaryDirectory.resolve("existing.sqlite3.enc");
        Files.writeString(output, "keep this file");
        char[] passphrase = "correct horse battery".toCharArray();

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseCli.backupDatabaseEncrypted(database, output, passphrase));
            assertEquals("keep this file", Files.readString(output));
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    @Test
    void encryptedBackupRejectsShortPassphrase() {
        Path output = backupDirectory.resolve("bad.sqlite3.enc");
        char[] shortPassphrase = "short".toCharArray();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseCli.backupDatabaseEncrypted(database, output, shortPassphrase));
        } finally {
            Arrays.fill(shortPassphrase, '\0');
        }
    }

    @Test
    void restoreEncryptedRejectsInvalidFile() throws Exception {
        Path invalid = temporaryDirectory.resolve("not-encrypted.enc");
        Files.writeString(invalid, "not encrypted");
        char[] passphrase = "correct horse battery".toCharArray();
        try {
            assertThrows(java.io.IOException.class,
                    () -> DatabaseCli.restoreEncryptedDatabase(database, invalid, backupDirectory, passphrase));
            assertEquals("current", bookingName(database));
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    @Test
    void restoreBacksUpCurrentDatabaseBeforeReplacingIt() throws Exception {
        Path backup = DatabaseCli.restoreDatabase(database, source, backupDirectory);

        assertNotNull(backup);
        assertTrue(Files.isRegularFile(backup));
        assertEquals("current", bookingName(backup));
        assertEquals("restored", bookingName(database));
    }

    @Test
    void restoreToNewDatabaseDoesNotCreatePreRestoreBackup() throws Exception {
        Path destination = temporaryDirectory.resolve("new/bookings.db");

        Path backup = DatabaseCli.restoreDatabase(destination, source, backupDirectory);

        assertNull(backup);
        assertEquals("restored", bookingName(destination));
        assertFalse(Files.exists(backupDirectory));
    }

    @Test
    void restoreRefusesDatabaseWithSqliteSidecar() throws Exception {
        Path sidecar = Path.of(database + "-wal");
        Files.createFile(sidecar);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> DatabaseCli.restoreDatabase(database, source, backupDirectory));

        assertTrue(error.getMessage().contains("sidecar files exist"));
        assertEquals("current", bookingName(database));
    }

    @Test
    void invalidRestoreSourceDoesNotChangeDatabase() throws Exception {
        Path invalid = temporaryDirectory.resolve("not-a-database.sqlite3");
        Files.writeString(invalid, "not sqlite");

        assertThrows(SQLException.class,
                () -> DatabaseCli.restoreDatabase(database, invalid, backupDirectory));

        assertEquals("current", bookingName(database));
    }

    private static void createDatabase(Path path, String name) throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE Booking (id TEXT PRIMARY KEY, name TEXT)");
            try (var insert = connection.prepareStatement(
                    "INSERT INTO Booking (id, name) VALUES ('1', ?)")) {
                insert.setString(1, name);
                insert.executeUpdate();
            }
        }
    }

    private static String bookingName(Path path) throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT name FROM Booking WHERE id = '1'")) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static void updateBookingName(Path path, String name) throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.prepareStatement("UPDATE Booking SET name = ? WHERE id = '1'")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }
}
