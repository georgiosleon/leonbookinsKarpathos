package com.leonsoft.desktop;

import java.awt.Desktop;
import java.io.Console;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class DesktopLauncher {
    private static final URI APPLICATION_URI = URI.create("http://127.0.0.1:8080");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run Leon Bookings from a terminal so credentials can be entered securely.");
            System.exit(1);
        }
        start(console);
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
            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable().toString(), "-jar", applicationJar.toString());
            builder.environment().put("APP_ADMIN_USERNAME", credentials.username());
            builder.environment().put("APP_ADMIN_PASSWORD", new String(credentials.password()));
            builder.environment().put("APP_DB_URL", "jdbc:sqlite:" + database);
            builder.environment().put("APP_DEPLOYMENT_TYPE", "desktop");
            builder.environment().put("APP_DIAGNOSTICS_LOG_FILE", logFile.toString());
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
            credentials.clear();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopServer(server),
                    "booking-server-shutdown-hook"));
            if (!waitForServer(server, console, logFile)) {
                stopServer(server);
                return;
            }
            console.printf("Database: %s%n", database);
            openBrowser(console);
            console.readLine("Leon Bookings is running. Press Enter to stop the service.");
            stopServer(server);
        } catch (IOException | RuntimeException error) {
            credentials.clear();
            console.printf("Could not start Leon Bookings: %s%n", error.getMessage());
        }
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
