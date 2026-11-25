package com.leonsoft.desktop;

import java.awt.Desktop;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DesktopLauncher {
    private static final URI APPLICATION_URI = URI.create("http://127.0.0.1:8080");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final String PRODUCTION_LICENSE_PUBLIC_KEY =
            "MCowBQYDK2VwAyEAmi1yDWu05LnXrnmFwnYS62X+QAl3xPWYvtYyBvHFjig=";
    private static final String BUILD_ID_RESOURCE = "/com/leonsoft/desktop/build-id.txt";
    private static final Path JAR_MARKER = Path.of("last-app-jar.sha256");
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS", Locale.ROOT);

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run Leon Bookings from a terminal so credentials can be entered securely.");
            System.exit(1);
        }
        if (args.length > 0 && isHelp(args[0])) {
            printUsage(console);
            return;
        }
        if (args.length > 0 && "backup".equalsIgnoreCase(args[0])) {
            runDatabaseMaintenance(console, "backup", args);
            return;
        }
        if (args.length > 0 && "restore".equalsIgnoreCase(args[0])) {
            runDatabaseMaintenance(console, "restore", args);
            return;
        }
        start(console);
    }

    private static boolean isHelp(String argument) {
        return "-h".equals(argument) || "--help".equals(argument) || "help".equalsIgnoreCase(argument);
    }

    private static void printUsage(Console console) {
        console.printf("Usage: LeonBookings [start|backup [file]|restore [file]]%n")
                .printf("  start             Start the local application (default).%n")
                .printf("  backup [file]     Create an encrypted backup (prompts for a passphrase).%n")
                .printf("  restore [file]    Restore an encrypted backup (prompts for confirmation and passphrase).%n");
    }

    private static void start(Console console) {
        Credentials credentials = askForCredentials(console);
        if (credentials == null) {
            return;
        }

        try {
            Path dataDirectory = dataDirectory();
            createPrivateDirectory(dataDirectory);
            createPrivateDirectory(dataDirectory.resolve("logs"));
            Path applicationJar = applicationJar();
            Path logFile = dataDirectory.resolve("logs/application.log");
            Path database = Path.of("bookings.db").toAbsolutePath().normalize();
            boolean newDatabase = !Files.exists(database);
            String jarFingerprint = jarFingerprint(applicationJar);
            createPreUpgradeBackup(console, dataDirectory, database, applicationJar, jarFingerprint);
            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable().toString(), "-jar", applicationJar.toString());
            builder.environment().put("APP_ADMIN_USERNAME", credentials.username());
            builder.environment().put("APP_ADMIN_PASSWORD", new String(credentials.password()));
            // Production rejects the built-in seed passwords. On a fresh database the
            // OPERATOR and GUEST accounts are placeholders; the administrator sets
            // usable passwords later in User Management.
            builder.environment().put("APP_OPERATOR_PASSWORD", randomSeedPassword());
            builder.environment().put("APP_GUEST_PASSWORD", randomSeedPassword());
            builder.environment().put("APP_DB_URL", "jdbc:sqlite:" + database);
            builder.environment().put("APP_DEPLOYMENT_TYPE", "desktop");
            builder.environment().put("APP_DIAGNOSTICS_LOG_FILE", logFile.toString());
            builder.environment().put("SPRING_PROFILES_ACTIVE", "prod");
            builder.environment().put("APP_LICENSE_BYPASS", "false");
            builder.environment().put("APP_LICENSE_PUBLIC_KEY", PRODUCTION_LICENSE_PUBLIC_KEY);
            String buildId = buildId();
            if (!buildId.isEmpty()) {
                builder.environment().put("APP_BUILD_ID", buildId);
            }
            if (newDatabase) {
                builder.environment().put(
                        "SPRING_SQL_INIT_DATA_LOCATIONS", "classpath:demo-data.sql");
            }
            builder.environment().putIfAbsent("SERVER_ADDRESS", "127.0.0.1");
            builder.environment().put("SERVER_PORT", "8080");
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());

            Process server = builder.start();
            builder.environment().remove("APP_ADMIN_PASSWORD");
            builder.environment().remove("APP_OPERATOR_PASSWORD");
            builder.environment().remove("APP_GUEST_PASSWORD");
            credentials.clear();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopServer(server),
                    "booking-server-shutdown-hook"));
            if (!waitForServer(server, console, logFile)) {
                stopServer(server);
                return;
            }
            Files.writeString(dataDirectory.resolve(JAR_MARKER), jarFingerprint);
            console.printf("Database: %s%n", database);
            openBrowser(console);
            console.readLine("Leon Bookings is running. Press Enter to stop the service.");
            stopServer(server);
        } catch (IOException | RuntimeException error) {
            credentials.clear();
            console.printf("Could not start Leon Bookings: %s%n", error.getMessage());
        }
    }

    private static void runDatabaseMaintenance(Console console, String operation, String[] args) {
        try {
            Path database = Path.of("bookings.db").toAbsolutePath().normalize();
            if (!Files.isRegularFile(database)) {
                console.printf("Database not found: %s%n", database);
                return;
            }
            List<String> command = databaseMaintenanceCommand(
                    console, operation, args, database, applicationJar());
            if (command == null) {
                return;
            }
            int exitCode = runCommand(command);
            if (exitCode != 0) {
                console.printf("%s failed (exit code %d).%n", operation, exitCode);
            }
        } catch (IOException | RuntimeException error) {
            console.printf("Could not %s the database: %s%n", operation, error.getMessage());
        }
    }

    private static List<String> databaseMaintenanceCommand(Console console, String operation,
                                                           String[] args, Path database,
                                                           Path applicationJar) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-jar");
        command.add(applicationJar.toString());
        command.add("db");
        command.add(operation);
        command.add("--database");
        command.add(database.toString());
        if ("backup".equals(operation)) {
            command.add("--encrypt");
            if (args.length > 1 && !args[1].isBlank()) {
                command.add("--output");
                command.add(args[1].trim());
            }
            return command;
        }
        String input = args.length > 1 ? args[1] : console.readLine("Encrypted backup file to restore: ");
        if (input == null || input.isBlank()) {
            return null;
        }
        console.printf("Make sure the application is stopped before restoring.%n");
        command.add("--input");
        command.add(input.trim());
        return command;
    }

    private static void createPreUpgradeBackup(Console console, Path dataDirectory, Path database,
                                               Path applicationJar, String fingerprint)
            throws IOException {
        Path marker = dataDirectory.resolve(JAR_MARKER);
        String previous = Files.isRegularFile(marker) ? Files.readString(marker).trim() : "";
        if (fingerprint.equals(previous) || !Files.isRegularFile(database)) {
            return;
        }
        Path backupDirectory = dataDirectory.resolve("backups");
        createPrivateDirectory(backupDirectory);
        Path output = backupDirectory.resolve("bookings-pre-upgrade-"
                + LocalDateTime.now().format(BACKUP_TIMESTAMP) + ".sqlite3");
        console.printf("New application package detected. Creating a local pre-upgrade backup...%n");
        List<String> command = List.of(
                javaExecutable().toString(), "-jar", applicationJar.toString(),
                "db", "backup", "--database", database.toString(), "--output", output.toString());
        int exitCode = runCommand(command);
        if (exitCode != 0) {
            throw new IOException("Pre-upgrade backup failed; startup aborted. See the messages above.");
        }
        console.printf("Pre-upgrade backup: %s%n", output);
    }

    private static int runCommand(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).inheritIO().start();
        try {
            return process.waitFor();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return 1;
        }
    }

    private static String jarFingerprint(Path jar) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(jar)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IOException("Could not fingerprint the application package.", error);
        }
    }

    private static String randomSeedPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Credentials askForCredentials(Console console) {
        console.printf("Leon Bookings setup%n");
        while (true) {
            String username = console.readLine("Administrator username [admin]: ");
            if (username == null) {
                return null;
            }
            username = username.trim();
            if (username.isEmpty()) {
                username = "admin";
            }
            char[] password = console.readPassword("Administrator password (12–72 UTF-8 bytes): ");
            if (password == null) {
                return null;
            }
            int passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8).length;
            if (passwordBytes >= 12 && passwordBytes <= 72) {
                return new Credentials(username, password);
            }
            java.util.Arrays.fill(password, '\0');
            console.printf("Password must be between 12 and 72 UTF-8 bytes.%n");
        }
    }

    private static boolean waitForServer(Process server, Console console, Path logFile) {
        long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline && server.isAlive()) {
            if (isApplicationReady()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                console.printf("Startup was interrupted. See %s%n", logFile);
                return false;
            }
        }
        console.printf("%s. See %s%n",
                server.isAlive()
                        ? "The application did not become ready"
                        : "The application stopped during startup",
                logFile);
        return false;
    }

    private static boolean isApplicationReady() {
        try {
            HttpURLConnection connection = (HttpURLConnection) APPLICATION_URI.resolve("/login")
                    .toURL().openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            connection.disconnect();
            return status >= 200 && status < 400;
        } catch (IOException error) {
            return false;
        }
    }

    private static void openBrowser(Console console) {
        try {
            if (!Desktop.isDesktopSupported()) {
                console.printf("Open %s in your browser.%n", APPLICATION_URI);
                return;
            }
            Desktop.getDesktop().browse(APPLICATION_URI);
        } catch (IOException error) {
            console.printf("Could not open a browser. Open %s manually.%n", APPLICATION_URI);
        }
    }

    private static void stopServer(Process server) {
        if (!server.isAlive()) {
            return;
        }
        server.destroy();
        try {
            if (!server.waitFor(10, TimeUnit.SECONDS)) {
                server.destroyForcibly();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            server.destroyForcibly();
        }
    }

    private static String buildId() {
        try (InputStream input = DesktopLauncher.class.getResourceAsStream(BUILD_ID_RESOURCE)) {
            if (input == null) {
                return "";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private static Path applicationJar() throws IOException {
        try {
            Path launcherJar = Path.of(DesktopLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path applicationJar = launcherJar.resolveSibling("leonbookinsKarpathos.jar");
            if (!Files.isRegularFile(applicationJar)) {
                throw new IOException("Application package not found beside the desktop launcher.");
            }
            return applicationJar;
        } catch (URISyntaxException error) {
            throw new IOException("Could not locate the installed application package.", error);
        }
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static Path dataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return (appData == null || appData.isBlank()
                    ? Path.of(System.getProperty("user.home"), "AppData", "Roaming")
                    : Path.of(appData)).resolve("LeonSoft").resolve("Leon Bookings");
        }
        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support",
                    "LeonSoft", "Leon Bookings");
        }
        String xdgData = System.getenv("XDG_DATA_HOME");
        return (xdgData == null || xdgData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".local", "share")
                : Path.of(xdgData)).resolve("leon-bookings");
    }

    private static void createPrivateDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        try {
            Files.setPosixFilePermissions(directory,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows protects user profile directories with user-specific ACLs.
        }
    }

    private record Credentials(String username, char[] password) {
        private void clear() {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
