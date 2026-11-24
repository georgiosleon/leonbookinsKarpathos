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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.sqlite.SQLiteConnection;

@SuppressWarnings({"PMD.SystemPrintln","PMD.AvoidReassigningParameters","PMD.UseVarargs","PMD.AvoidDuplicateLiterals","PMD.AssignmentInOperand","PMD.AvoidReassigningLoopVariables","PMD.AvoidLiteralsInIfCondition","PMD.CloseResource","PMD.PreserveStackTrace"})
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
                    options.allowOnly("--database", "--output", "--encrypt");
                    boolean encrypt = options.flag("--encrypt");
                    Path output = options.path("--output", timestampedPath(
                            DEFAULT_BACKUP_DIRECTORY,
                            "bookings",
                            encrypt ? ".sqlite3.enc" : ".sqlite3"));
                    Path result;
                    if (encrypt) {
                        char[] passphrase = requestBackupPassphrase();
                        try {
                            result = backupDatabaseEncrypted(database, output, passphrase);
                        } finally {
                            Arrays.fill(passphrase, '\0');
                        }
                    } else {
                        result = backupDatabase(database, output);
                    }
                    System.out.println("Backup created: " + result);
                }
                case "restore" -> {
                    options.allowOnly("--database", "--input", "--backup-dir", "--yes");
                    Path input = options.requiredPath("--input");
                    if (!options.flag("--yes")) {
                        confirmRestore(database, input);
                    }
                    Path backup;
                    if (EncryptedDatabaseBackup.isEncrypted(input)) {
                        char[] passphrase = requestRestorePassphrase();
                        try {
                            backup = restoreEncryptedDatabase(
                                    database,
                                    input,
                                    options.path("--backup-dir", DEFAULT_BACKUP_DIRECTORY),
                                    passphrase);
                        } finally {
                            Arrays.fill(passphrase, '\0');
                        }
                    } else {
                        backup = restoreDatabase(
                                database,
                                input,
                                options.path("--backup-dir", DEFAULT_BACKUP_DIRECTORY));
                    }
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

        Path parent = requireParent(output);
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

    public static Path backupDatabaseEncrypted(Path database, Path output, char[] passphrase)
            throws IOException, SQLException {
        database = database.toAbsolutePath().normalize();
        output = output.toAbsolutePath().normalize();
        if (database.equals(output)) {
            throw new IllegalArgumentException("Backup output must be different from the live database");
        }
        validateDatabase(database);

        Path parent = requireParent(output);
        Files.createDirectories(parent);
        Path temporaryDatabase = null;
        Path temporaryEncrypted = null;
        try {
            temporaryDatabase = Files.createTempFile(parent, ".booking-", ".sqlite3");
            temporaryEncrypted = Files.createTempFile(parent, ".booking-", ".enc");
            Files.delete(temporaryDatabase);
            backupDatabase(database, temporaryDatabase);
            validateDatabase(temporaryDatabase);
            EncryptedDatabaseBackup.encrypt(temporaryDatabase, temporaryEncrypted, passphrase);
            try {
                Files.move(temporaryEncrypted, output);
            } catch (java.nio.file.FileAlreadyExistsException error) {
                throw new IllegalArgumentException("Refusing to overwrite existing backup: " + output, error);
            }
        } finally {
            if (temporaryDatabase != null) {
                Files.deleteIfExists(temporaryDatabase);
            }
            if (temporaryEncrypted != null) {
                Files.deleteIfExists(temporaryEncrypted);
            }
        }
        return output;
    }

    public static Path restoreEncryptedDatabase(
            Path database, Path input, Path backupDirectory, char[] passphrase)
            throws IOException, SQLException {
        database = database.toAbsolutePath().normalize();
        input = input.toAbsolutePath().normalize();
        if (database.equals(input)) {
            throw new IllegalArgumentException("Restore source must be different from the live database");
        }
        Path parent = requireParent(database);
        Files.createDirectories(parent);
        Path temporaryDatabase = Files.createTempFile(parent, ".booking-restore-", ".sqlite3");
        try {
            EncryptedDatabaseBackup.decrypt(input, temporaryDatabase, passphrase);
            validateDatabase(temporaryDatabase);
            return restoreDatabase(database, temporaryDatabase, backupDirectory);
        } finally {
            Files.deleteIfExists(temporaryDatabase);
        }
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

        Path parent = requireParent(database);
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(
                parent, "." + database.getFileName() + ".restore-", ".sqlite3");
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
        return timestampedPath(directory, prefix, ".sqlite3");
    }

    private static Path timestampedPath(Path directory, String prefix, String extension) {
        return directory.resolve(prefix + "-" + LocalDateTime.now().format(TIMESTAMP) + extension);
    }

    private static char[] requestBackupPassphrase() {
        if (System.console() == null) {
            throw new IllegalArgumentException(
                    "Encrypted backup requires an interactive terminal for the passphrase");
        }
        char[] passphrase = System.console().readPassword("New backup passphrase (at least 12 characters): ");
        if (passphrase == null) {
            throw new IllegalArgumentException("Encrypted backup cancelled");
        }
        if (passphrase.length < 12) {
            Arrays.fill(passphrase, '\0');
            throw new IllegalArgumentException("Backup passphrase must be at least 12 characters");
        }
        char[] confirmation = System.console().readPassword("Confirm backup passphrase: ");
        if (!Arrays.equals(passphrase, confirmation)) {
            Arrays.fill(passphrase, '\0');
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
            throw new IllegalArgumentException("Backup passphrases do not match");
        }
        Arrays.fill(confirmation, '\0');
        return passphrase;
    }

    private static char[] requestRestorePassphrase() {
        if (System.console() == null) {
            throw new IllegalArgumentException(
                    "Encrypted restore requires an interactive terminal for the passphrase");
        }
        char[] passphrase = System.console().readPassword("Backup passphrase: ");
        if (passphrase == null) {
            throw new IllegalArgumentException("Encrypted restore cancelled");
        }
        return passphrase;
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

    private static Path requireParent(Path path) {
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Path must have a parent directory: " + path);
        }
        return parent;
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  java -jar app.jar db backup [--database PATH] [--output PATH] [--encrypt]
                  java -jar app.jar db restore --input PATH [--database PATH]
                      [--backup-dir PATH] [--yes]

                Defaults:
                  Database:   bookings.db (current working directory)
                  Backups:    backups/ (current working directory)

                  Encrypted backups prompt for a passphrase; restore detects them automatically.
                  Stop the application before restoring a database.
                  """);
    }

    private static Options parseOptions(String[] args, int start) {
        Options options = new Options();
        for (int index = start; index < args.length; index++) {
            String name = args[index];
            if ("--yes".equals(name) || "--encrypt".equals(name)) {
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
        private final java.util.Set<String> flags = new java.util.HashSet<>();

        private void addValue(String name, String value) {
            if (!List.of("--database", "--output", "--input", "--backup-dir").contains(name)) {
                throw new IllegalArgumentException("Unknown option: " + name);
            }
            if (values.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Option specified more than once: " + name);
            }
        }

        private void addFlag(String name) {
            if (!List.of("--yes", "--encrypt").contains(name) || !flags.add(name)) {
                throw new IllegalArgumentException("Invalid or repeated option: " + name);
            }
        }

        private boolean flag(String name) {
            return flags.contains(name);
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
            for (String name : flags) {
                if (!allowedOptions.contains(name)) {
                    throw new IllegalArgumentException("Option is not valid for this command: " + name);
                }
            }
        }
    }
}
