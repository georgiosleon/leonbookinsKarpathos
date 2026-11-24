# Desktop installers

This folder contains the desktop launcher and native installer build scripts.
The command-line launcher prompts for administrator credentials, runs the
Spring Boot app locally, opens the React app in the default browser, and stops
the server when you press Enter or interrupt the launcher. The launcher passes
credentials to the application process as `APP_ADMIN_*` environment variables;
it does not save them to disk. User accounts are stored in the SQLite database
and managed by admins in the application. Start the app image or installed
launcher from a terminal; it requires an interactive console.

Application logs are kept in the current user's application data directory:

- Windows: `%APPDATA%\LeonSoft\Leon Bookings`
- macOS: `~/Library/Application Support/LeonSoft/Leon Bookings`
- Linux: `${XDG_DATA_HOME:-~/.local/share}/leon-bookings`

The desktop launcher uses `bookings.db` in its current working directory by
default. Startup uses JPA schema updates and `DatabaseInitializer` for
idempotent seed/migration logic (including legacy role normalization). Back up
the database before moving, upgrading, or replacing it; future schema-altering
changes should use versioned migrations.

For a support copy, use `db backup --encrypt` to create a passphrase-protected
`.sqlite3.enc` file; the full Windows and macOS commands are in the app's Help
dialog. Send the encrypted file and communicate its passphrase separately
through a trusted channel. The passphrase cannot be recovered. The app detects
encrypted files during restore, which must be done after stopping the service.
The app does not email or upload backups. Use the approved support channel and
never send the passphrase in the same message as the backup.

The installer scripts check prerequisites before building and stop with an
actionable error if a required command or native packaging tool is missing.
Build prerequisites are JDK 25 (including `javac` and `jpackage`), the
project's Maven Wrapper, and the platform's native package tools. The wrapper
downloads the pinned Maven distribution on first use. The build downloads and
packages the frontend through Maven; end users do not need Java, Maven,
Node.js, npm, or Docker installed.

## Accounts and credentials

The application uses database-backed users with roles `ADMIN`, `OPERATOR`, and
`GUEST` (legacy `USER` accepted/migrated to `OPERATOR`). Seed accounts are
created only when missing.

- **Desktop installer:** the launcher prompts for admin credentials and passes
  them as `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD`. Configure operator/guest
  seed accounts via `APP_OPERATOR_*` (or legacy `APP_USER_*`) and `APP_GUEST_*`
  environment variables as needed.
- **Docker:** set `APP_ADMIN_*`, `APP_OPERATOR_*` (or legacy `APP_USER_*`), and
  `APP_GUEST_*` in the private `.env` file before running Compose. Replace
  placeholder passwords.
- **Direct Spring Boot/Maven run:** set the same environment variables in the
  shell before starting the application.

Seed passwords must be at least 12 characters. Keep environment files and
shell/session access private; do not commit real credentials.

## Install build prerequisites

### Linux (Fedora)

For Fedora, the script defaults to a portable Linux application archive and
can install its build dependencies with `dnf`:

```sh
./install/package.sh --install-deps
```

This installs JDK 25 and its module files (`jmods`) through `dnf` (using
`sudo` if needed). The wrapper downloads Maven when it first runs. The JMOD
files are required by `jpackage`/`jlink` to
create the app's bundled Java runtime. Fedora's default output is a portable
Linux app archive, so it does not require `dpkg` or RPM build tools. To build a
Debian package on Fedora, request it explicitly; the script also installs
`dpkg`:

```sh
./install/package.sh deb --install-deps
```

If this terminal cannot detect Fedora, specify the package manager; it will
select the portable Linux archive automatically:

```sh
./install/package.sh --install-deps --manager dnf
```

The selected package manager must be available in the current shell. If the
script says `dnf` is unavailable, you may be in a container/Flatpak shell
rather than a Fedora host terminal; run the command in Fedora's Terminal or
install dependencies on the host first.

Debian and Ubuntu default to `.deb` and can use `apt-get`. To build the
portable archive instead, request `linux`:

```sh
./install/package.sh deb --install-deps --manager apt-get
./install/package.sh linux
```

This installs `openjdk-25-jdk` and `dpkg-dev` for `.deb` builds. If your
distro's repositories do not provide JDK 25, install a JDK 25 distribution
that includes `jpackage` (such as Eclipse Temurin) manually, then rerun the
script.

To only check prerequisites without installing packages, omit
`--install-deps`. Confirm the commands resolve to JDK 25:

```sh
java -version
javac -version
jpackage --version
./mvnw --version
```

For `.deb` builds, also verify `dpkg-deb --version`; for portable Linux
archives, verify `tar --version`.

### macOS

Install missing build dependencies with Homebrew:

```sh
./install/package.sh --install-deps
```

The script runs `brew install --cask temurin@25`. Maven is supplied by the
project wrapper, which downloads Maven 3.9.11 on first use. Install Homebrew
first if it is unavailable. If Homebrew does not have the
requested Temurin cask, install JDK 25 from Adoptium and ensure its `bin`
directory is on `PATH`. `jpackage` is included in the JDK; `hdiutil` is part
of macOS.

```sh
java -version
javac -version
jpackage --version
./mvnw --version
hdiutil -help
```

### Windows 11

Install the JDK from PowerShell:

```powershell
winget install --id EclipseAdoptium.Temurin.25.JDK --exact
```

Close and reopen PowerShell so the installer changes are loaded. Maven 3.9.11
is downloaded by `mvnw.cmd` on first use. Set `JAVA_HOME` to the JDK 25
installation if the installer did not set it. The MSI builder also needs
**WiX Toolset 3.14.x** (WiX 3, not WiX 4 or newer).
Install it from the official WiX Toolset 3 release and add the directory
containing `candle.exe` and `light.exe` to `PATH`, then reopen PowerShell.

```powershell
java -version
javac -version
jpackage --version
.\mvnw.cmd --version
Get-Command candle.exe, light.exe
```

The Java tools must all report version 25. The wrapper reports the pinned
Maven 3.9.11 version. If multiple JDKs are installed, set `JAVA_HOME` and put
the JDK 25 `bin` directory before older JDK directories in `PATH`.

The JDK used by `java` must also contain a `jmods/` directory on every build
platform. The Linux, macOS, and Windows scripts check for it. If installer
creation fails with a `jlink` message saying the JDK has no packaged modules,
install the full JDK module package and rerun the build. On Fedora:

```sh
sudo dnf install java-25-openjdk-jmods
./install/package.sh
```

On macOS or Windows, use a full JDK 25 development installation that includes
`jmods` (such as Eclipse Temurin 25), and ensure its `bin` directory is first
on `PATH`.

Build on each target operating system (packages are not cross-built).
Linux automatically selects a portable archive for Fedora/RHEL with `dnf` and
`.deb` for Debian/Ubuntu with `apt-get`; choose `linux` for a portable archive
on any Linux distribution:

```sh
# Debian/Ubuntu package
./install/package.sh deb

# Portable Linux archive with bundled Java runtime
./install/package.sh linux

# macOS
./install/package.sh dmg
./install/package.sh portable

# Windows PowerShell
.\install\package.ps1 msi
.\install\package.ps1 portable
```

Each package contains a Java 25 runtime linked from the modules used by the
application, rather than a full JDK or every module in the build JDK. The
runtime retains its `bin/java` command because the launcher uses it to start
the Spring Boot server. The Linux workflow starts the bundled application as
a smoke test. The launcher prints the working-directory database path at
startup; application logs are stored separately in the operating system's
application-data directory documented above.

The portable Linux archive is written to
`install/dist/linux/LeonBookings-<version>-linux-<architecture>.tar.gz`.
From the directory containing the archive, extract it, enter the app image,
and launch it from a terminal:

```sh
tar -xzf LeonBookings-2.0.2-linux-x86_64.tar.gz
cd LeonBookings
./bin/LeonBookings
```

Replace the example archive name with the version and architecture you
downloaded. The app image includes its Java runtime and does not require a
Linux package manager. The launcher prompts for administrator credentials,
starts the local service, and opens it in your browser. Keep the terminal open
while using the app; pressing Enter stops the service. The launcher prints the
database path at startup; logs are stored under
`${XDG_DATA_HOME:-~/.local/share}/leon-bookings`.
Debian/Ubuntu can also build `.deb` packages under `install/dist/deb/`.

The macOS DMG is written to `install/dist/dmg/` and contains `LeonBookings.app`.
Launch it from Terminal with
`/Applications/LeonBookings.app/Contents/MacOS/LeonBookings`; opening it from
Finder does not provide the interactive console required by the command-line
launcher.
The Windows MSI is written to `install/dist/msi/` and creates a `LeonBookings`
Start Menu entry and shortcut that open a console for the command-line
launcher. The native package name intentionally has no spaces.

Portable Windows and macOS builds are self-contained ZIP archives under
`install/dist/portable/`. Build them with `.\install\package.ps1 portable` on
Windows or `./install/package.sh portable` on macOS. They include the
application image and bundled Java runtime; they do not install files or
create Start Menu entries. Extract the archive and launch from a terminal in
the extracted directory so the working-directory `bookings.db` stays beside
the portable copy:

```powershell
cd .\LeonBookings
.\LeonBookings.exe
```

```sh
./LeonBookings.app/Contents/MacOS/LeonBookings
```

Run that command from the directory where you extracted the archive. The
macOS archive is built for the architecture of the build machine; Intel and
Apple Silicon Macs need matching builds. Portable archives are not signed or
notarized, so Windows SmartScreen or macOS Gatekeeper may show warnings.
The Windows output name is
`LeonBookings-<version>-windows-<architecture>.zip`; the macOS output name is
`LeonBookings-<version>-macos-<architecture>.zip`.

The optional `--install-deps` flag is supported on Linux (`dnf`/`apt-get`) and
macOS (Homebrew); it installs only the build prerequisites required by the
chosen package format. Use `--manager` if automatic package-manager detection
doesn't work. Package managers may request administrator approval.
Windows prerequisites are checked by `package.ps1` and are installed
separately as described above.

Each build runs `./mvnw -B package` (or `mvnw.cmd` on Windows, including the
Maven test phase), compiles the Java launcher, and creates the package
under `install/dist/`. The first wrapper run downloads Maven 3.9.11 and
verifies its SHA-512 checksum; later runs use the local Maven cache. The `dist`
directory is generated output and is intentionally ignored by Git; this
folder's source, scripts, and instructions are tracked.

Desktop installer GitHub Actions automation has been removed. Build installers
locally on the matching operating system using the scripts above.

To publish a release manually, update the project version in `pom.xml`, commit
that change, then create and push the matching tag, for example:

```sh
git tag v1.0.1
git push origin v1.0.1
```

Maven Wrapper's Maven version is a separate build-tool pin; it does not need to
change for an application release.

The application binds to `127.0.0.1:8080` by default. To allow other devices
on the LAN to connect, set `SERVER_ADDRESS=0.0.0.0` in the environment before
launching it; then open `http://<host-computer-LAN-IP>:8080` from the other
device. This exposes the app to every network interface, so enable it only on a
trusted network and restrict port 8080 with the host firewall. Use strong
account passwords. The launcher reports the startup log location in the
user's data directory if the port is already in use.

For Docker Compose, the published port is loopback-only by default. To enable
LAN access, change `BOOKING_HOST_ADDRESS` in `.env` to `0.0.0.0`; keep the host
firewall restricted to trusted devices.

Portable launcher examples:

```powershell
# Windows PowerShell, from the extracted portable folder
$env:SERVER_ADDRESS = "0.0.0.0"
.\LeonBookings.exe
```

```sh
# Linux, from the extracted application image
SERVER_ADDRESS=0.0.0.0 ./bin/LeonBookings

# macOS, from the extracted portable folder
SERVER_ADDRESS=0.0.0.0 ./LeonBookings.app/Contents/MacOS/LeonBookings
```

To restore local-only desktop access, start a new terminal without the
`SERVER_ADDRESS` override. To restore local-only Docker access, set
`BOOKING_HOST_ADDRESS=127.0.0.1` in `.env` and run `docker compose up -d`.

The database remains on the computer running the app. Do not open one live
SQLite database file from multiple computers over a network share. Stop the
app and use its backup/restore commands to move or exchange database files.

On macOS, a smooth public release still requires Apple signing and notarization.
Windows MSI packaging requires WiX Toolset 3 on `PATH`.
The MSI uses a stable upgrade identifier so newer versions can upgrade the
existing application without removing the user's database.

## API versions

The current frontend uses `/api/v1` routes, including
`/api/v1/booking/...` and `/api/v1/csrf`. Existing `/booking/...` and `/csrf`
routes remain available for compatibility. Changes that break clients should
be introduced under a new major path such as `/api/v2`; do not silently change
the meaning or shape of `/api/v1` responses.
