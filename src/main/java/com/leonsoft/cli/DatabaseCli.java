package com.leonsoft.cli;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.sqlite.SQLiteConnection;

public final class DatabaseCli {
    private static final Path DEFAULT_DATABASE = Path.of("bookings.db");
    private static final Path DEFAULT_BACKUP_DIRECTORY = Path.of("backups");
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS", Locale.ROOT);

    private DatabaseCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        try {
            if (args.length == 0 || isHelp(args[0])) {
                printUsage();
                return args.length == 0 ? 2 : 0;
            }

            String operation = args[0];
            Options options = parseOptions(args, 1);
            Path database = options.path("--database", DEFAULT_DATABASE);

            switch (operation) {
                case "backup" -> {
                    options.allowOnly("--database", "--output");
                    Path output = options.path("--output",
                            timestampedPath(DEFAULT_BACKUP_DIRECTORY, "bookings"));
                    Path result = backupDatabase(database, output);
                    System.out.println("Backup created: " + result);
                }
                case "restore" -> {
                    options.allowOnly("--database", "--input", "--backup-dir", "--yes");
                    Path input = options.requiredPath("--input");
                    if (!options.flag("--yes")) {
                        confirmRestore(database, input);
                    }
                    Path backup = restoreDatabase(
                            database,
                            input,
                            options.path("--backup-dir", DEFAULT_BACKUP_DIRECTORY));
                    if (backup != null) {
                        System.out.println("Pre-restore backup created: " + backup);
                    }
                    System.out.println("Database restored: " + database);
                }
                default -> throw new IllegalArgumentException("Unknown database command: " + operation);
            }
            return 0;
        } catch (IOException | SQLException | IllegalArgumentException error) {
            System.err.println("ERROR: " + error.getMessage());
            return 1;
        }
    }

    public static Path backupDatabase(Path database, Path output) throws IOException, SQLException {
        database = database.toAbsolutePath().normalize();
        output = output.toAbsolutePath().normalize();
        if (database.equals(output)) {
            throw new IllegalArgumentException("Backup output must be different from the live database");
        }
        validateDatabase(database);

        Path parent = output.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + output.getFileName() + ".", ".sqlite3");
        try {
            copyDatabase(database, temporary);
            validateDatabase(temporary);
            try {
                Files.move(temporary, output);
            } catch (java.nio.file.FileAlreadyExistsException error) {
                throw new IllegalArgumentException("Refusing to overwrite existing backup: " + output, error);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return output;
    }

    public static Path restoreDatabase(Path database, Path input, Path backupDirectory)
            throws IOException, SQLException {
        database = database.toAbsolutePath().normalize();
        input = input.toAbsolutePath().normalize();
        backupDirectory = backupDirectory.toAbsolutePath().normalize();

        if (Files.isSymbolicLink(database)) {
            throw new IllegalArgumentException("Refusing to replace a symbolic-link database: " + database);
        }
        if (database.equals(input)) {
            throw new IllegalArgumentException("Restore source must be different from the live database");
        }
        validateDatabase(input);
        if (Files.exists(database) && !Files.isRegularFile(database)) {
            throw new IllegalArgumentException("Database target is not a regular file: " + database);
        }
        List<Path> sidecars = existingSidecars(database);
        if (!sidecars.isEmpty()) {
            throw new IllegalArgumentException(
                    "SQLite sidecar files exist; stop the app and resolve them before restoring: "
                            + String.join(", ", sidecars.stream().map(Path::toString).toList()));
        }

        Files.createDirectories(database.getParent());
        Path temporary = Files.createTempFile(
                database.getParent(), "." + database.getFileName() + ".restore-", ".sqlite3");
        Path preRestoreBackup = null;
        try {
            copyDatabase(input, temporary);
            validateDatabase(temporary);
            if (Files.exists(database)) {
                Files.createDirectories(backupDirectory);
                preRestoreBackup = backupDatabase(
                        database,
                        timestampedPath(backupDirectory, database.getFileName() + "-pre-restore"));
            }
            replaceDatabase(temporary, database);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return preRestoreBackup;
    }

    private static void validateDatabase(Path database) throws SQLException {
        if (!Files.isRegularFile(database)) {
            throw new IllegalArgumentException("Database file does not exist: " + database);
        }
        try (var connection = DriverManager.getConnection(readOnlyUrl(database))) {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1)) || result.next()) {
                    throw new IllegalArgumentException("SQLite integrity check failed for " + database);
                }
            }
            try (var statement = connection.createStatement();
                 var table = statement.executeQuery(
                         "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'Booking' COLLATE NOCASE")) {
                if (!table.next()) {
                    throw new IllegalArgumentException(
                            database + " does not contain the application's Booking table");
                }
            }
        }
    }

    private static void copyDatabase(Path source, Path destination) throws SQLException {
        try (var connection = DriverManager.getConnection(readOnlyUrl(source))) {
            if (!(connection instanceof SQLiteConnection sqliteConnection)) {
                throw new SQLException("The configured JDBC driver does not support SQLite backup");
            }
            int result = sqliteConnection.getDatabase().backup("main", destination.toString(), null);
            if (result != 0) {
                throw new SQLException("SQLite backup failed with result code " + result);
            }
        }
    }

    private static String readOnlyUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath().normalize().toUri() + "?mode=ro";
    }

    private static List<Path> existingSidecars(Path database) {
        List<Path> existing = new ArrayList<>();
        for (String suffix : List.of("-wal", "-shm", "-journal")) {
            Path sidecar = Path.of(database + suffix);
            if (Files.exists(sidecar)) {
                existing.add(sidecar);
            }
        }
        return existing;
    }

    private static void replaceDatabase(Path temporary, Path database) throws IOException {
        try {
            Files.move(temporary, database, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporary, database, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path timestampedPath(Path directory, String prefix) {
        return directory.resolve(prefix + "-" + LocalDateTime.now().format(TIMESTAMP) + ".sqlite3");
    }

    private static void confirmRestore(Path database, Path input) {
        if (System.console() == null) {
            throw new IllegalArgumentException(
                    "Restore requires an interactive terminal; pass --yes to confirm explicitly");
        }
        System.out.println("Restore " + input + " over " + database
                + "? The application must be stopped.");
        String confirmation = System.console().readLine("Type RESTORE to continue: ");
        if (!"RESTORE".equals(confirmation == null ? null : confirmation.trim())) {
            throw new IllegalArgumentException("Restore cancelled");
        }
    }

    private static boolean isHelp(String value) {
        return "-h".equals(value) || "--help".equals(value) || "help".equals(value);
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  java -jar app.jar db backup [--database PATH] [--output PATH]
                  java -jar app.jar db restore --input PATH [--database PATH]
                      [--backup-dir PATH] [--yes]

                Defaults:
                  Database:   bookings.db (current working directory)
                  Backups:    backups/ (current working directory)

                Stop the application before restoring a database.
                """);
    }

    private static Options parseOptions(String[] args, int start) {
        Options options = new Options();
        for (int index = start; index < args.length; index++) {
            String name = args[index];
            if ("--yes".equals(name)) {
                options.addFlag(name);
                continue;
            }
            if (!name.startsWith("--") || index + 1 >= args.length) {
                throw new IllegalArgumentException("Invalid or incomplete option: " + name);
            }
            String value = args[++index];
            if (value.isBlank()) {
                throw new IllegalArgumentException("Option value cannot be blank: " + name);
            }
            options.addValue(name, value);
        }
        return options;
    }

    private static final class Options {
        private final java.util.Map<String, String> values = new java.util.HashMap<>();
        private boolean confirmed;

        private void addValue(String name, String value) {
            if (!List.of("--database", "--output", "--input", "--backup-dir").contains(name)) {
                throw new IllegalArgumentException("Unknown option: " + name);
            }
            if (values.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Option specified more than once: " + name);
            }
        }

        private void addFlag(String name) {
            if (!"--yes".equals(name) || confirmed) {
                throw new IllegalArgumentException("Invalid or repeated option: " + name);
            }
            confirmed = true;
        }

        private boolean flag(String name) {
            return "--yes".equals(name) && confirmed;
        }

        private Path path(String name, Path fallback) {
            return Path.of(values.getOrDefault(name, fallback.toString()));
        }

        private Path requiredPath(String name) {
            String value = values.get(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required option: " + name);
            }
            return Path.of(value);
        }

        private void allowOnly(String... allowed) {
            List<String> allowedOptions = List.of(allowed);
            for (String name : values.keySet()) {
                if (!allowedOptions.contains(name)) {
                    throw new IllegalArgumentException("Option is not valid for this command: " + name);
                }
            }
            if (confirmed && !allowedOptions.contains("--yes")) {
                throw new IllegalArgumentException("Option is not valid for this command: --yes");
            }
        }
    }
}
