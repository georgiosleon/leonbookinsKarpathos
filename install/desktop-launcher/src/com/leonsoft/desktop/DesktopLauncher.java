package com.leonsoft.desktop;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class DesktopLauncher {
    private static final URI APPLICATION_URI = URI.create("http://127.0.0.1:8080");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DesktopLauncher::start);
    }

    private static void start() {
        Credentials credentials = askForCredentials();
        if (credentials == null) {
            return;
        }

        try {
            Path dataDirectory = dataDirectory();
            createPrivateDirectory(dataDirectory);
            createPrivateDirectory(dataDirectory.resolve("logs"));
            Path applicationJar = applicationJar();
            Path logFile = dataDirectory.resolve("logs/application.log");
            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable().toString(), "-jar", applicationJar.toString());
            builder.environment().put("APP_ADMIN_USERNAME", credentials.username());
            builder.environment().put("APP_ADMIN_PASSWORD", new String(credentials.password()));
            builder.environment().put("APP_DB_URL",
                    "jdbc:sqlite:" + dataDirectory.resolve("bookings.db").toAbsolutePath());
            builder.environment().put("SERVER_ADDRESS", "127.0.0.1");
            builder.environment().put("SERVER_PORT", "8080");
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());

            Dashboard dashboard = new Dashboard();
            Process server = builder.start();
            builder.environment().remove("APP_ADMIN_PASSWORD");
            credentials.clear();
            Runtime.getRuntime().addShutdownHook(new Thread(server::destroy, "booking-server-shutdown-hook"));
            dashboard.setServer(server);
            dashboard.show();
            waitForServer(server, dashboard, logFile);
        } catch (IOException | RuntimeException error) {
            credentials.clear();
            showError("Could not start Leon Bookings: " + error.getMessage());
        }
    }

    private static Credentials askForCredentials() {
        JTextField username = new JTextField("admin", 24);
        JPasswordField password = new JPasswordField(24);
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 6));
        fields.add(new JLabel("Administrator username"));
        fields.add(username);
        fields.add(new JLabel("Administrator password (12–72 UTF-8 bytes)"));
        fields.add(password);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    null, fields, "Set up Leon Bookings",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }
            String name = username.getText().trim();
            char[] passwordChars = password.getPassword();
            int passwordBytes = new String(passwordChars).getBytes(StandardCharsets.UTF_8).length;
            if (!name.isBlank() && passwordBytes >= 12 && passwordBytes <= 72) {
                return new Credentials(name, passwordChars);
            }
            java.util.Arrays.fill(passwordChars, '\0');
            JOptionPane.showMessageDialog(
                    null,
                    "Enter a username and a password between 12 and 72 UTF-8 bytes.",
                    "Check administrator credentials",
                    JOptionPane.WARNING_MESSAGE);
            password.setText("");
        }
    }

    private static void waitForServer(Process server, Dashboard dashboard, Path logFile) {
        Thread startup = new Thread(() -> {
            long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline && server.isAlive()) {
                if (isApplicationReady()) {
                    SwingUtilities.invokeLater(() -> {
                        dashboard.setStatus("Running — the booking app opened in your browser.");
                        openBrowser(dashboard);
                    });
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            String message = server.isAlive()
                    ? "The application did not become ready. See " + logFile
                    : "The application stopped during startup. See " + logFile;
            SwingUtilities.invokeLater(() -> dashboard.setStatus(message));
        }, "booking-server-startup");
        startup.setDaemon(true);
        startup.start();
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

    private static void openBrowser(Dashboard dashboard) {
        try {
            if (!Desktop.isDesktopSupported()) {
                dashboard.setStatus("Running — open http://127.0.0.1:8080 in your browser.");
                return;
            }
            Desktop.getDesktop().browse(APPLICATION_URI);
        } catch (IOException error) {
            dashboard.setStatus("Running — open http://127.0.0.1:8080 in your browser.");
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

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Leon Bookings", JOptionPane.ERROR_MESSAGE);
    }

    private record Credentials(String username, char[] password) {
        private void clear() {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static final class Dashboard {
        private final JFrame frame = new JFrame("Leon Bookings");
        private final JLabel status = new JLabel("Starting the local booking service…");
        private final JButton openButton = new JButton("Open booking app");
        private Process server;

        private Dashboard() {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(420, 150);
            frame.setLocationRelativeTo(null);
            frame.add(status, BorderLayout.CENTER);

            JPanel buttons = new JPanel();
            openButton.setEnabled(false);
            openButton.addActionListener(event -> openBrowser(this));
            JButton stopButton = new JButton("Stop");
            stopButton.addActionListener(event -> frame.dispose());
            buttons.add(openButton);
            buttons.add(stopButton);
            frame.add(buttons, BorderLayout.SOUTH);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    stopServer();
                }
            });
        }

        private void show() {
            frame.setVisible(true);
        }

        private void setServer(Process process) {
            server = process;
            process.onExit().thenRun(() ->
                    SwingUtilities.invokeLater(() -> setStatus("The booking service has stopped.")));
        }

        private void setStatus(String message) {
            status.setText(message);
            openButton.setEnabled(server != null && server.isAlive());
        }

        private void stopServer() {
            if (server == null || !server.isAlive()) {
                return;
            }
            server.destroy();
            Thread shutdown = new Thread(() -> {
                try {
                    if (!server.waitFor(10, TimeUnit.SECONDS)) {
                        server.destroyForcibly();
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    server.destroyForcibly();
                }
            }, "booking-server-shutdown");
            shutdown.setDaemon(true);
            shutdown.start();
        }
    }
}
