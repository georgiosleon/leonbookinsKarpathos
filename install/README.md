# Desktop installers

This folder contains the desktop launcher and native installer build scripts.
The Java desktop launcher prompts for administrator credentials, runs the
Spring Boot app locally, opens the React app in the default browser, and stops
the server when the launcher window closes. Credentials are requested on each
launch. Enter a username and a password of 12–72 UTF-8 bytes. The launcher
passes them to the application process; it does not save them to disk or the
database.

The database and application log are kept in the current user's application
data directory:

- Windows: `%APPDATA%\LeonSoft\Leon Bookings`
- macOS: `~/Library/Application Support/LeonSoft/Leon Bookings`
- Linux: `${XDG_DATA_HOME:-~/.local/share}/leon-bookings`

The installer scripts check prerequisites before building and stop with an
actionable error if a required command or native packaging tool is missing.
Build prerequisites are JDK 25 (including `javac` and `jpackage`), the
project's Maven Wrapper, and the platform's native package tools. The wrapper
downloads the pinned Maven distribution on first use. The build downloads and
packages the frontend through Maven; end users do not need Java, Maven,
Node.js, npm, or Docker installed.

## Administrator credentials

The application currently provisions one in-memory administrator account when
it starts. It does not keep user accounts or passwords in SQLite.

- **Desktop installer:** enter the administrator username and password in the
  launcher prompt on each launch.
- **Docker:** set `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` in the private
  `.env` file before running Compose. The sample `.env.example` contains a
  placeholder password that must be replaced.
- **Direct Spring Boot/Maven run:** set those two environment variables in
  the shell before starting the application.

Passwords must be between 12 and 72 UTF-8 bytes. Keep environment files and
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

# Windows PowerShell
./install/package.ps1 msi
```

The portable Linux archive is written to
`install/dist/linux/LeonBookings-<version>-linux-<architecture>.tar.gz`.
Extract it, then run `./LeonBookings/bin/LeonBookings` from the directory
containing the extracted `LeonBookings` folder. The app image includes its
Java runtime and does not require a Linux package manager. Debian/Ubuntu can
also build `.deb` packages under `install/dist/deb/`.

The macOS DMG is written to `install/dist/dmg/` and contains `LeonBookings.app`.
The Windows MSI is written to `install/dist/msi/` and creates a `LeonBookings`
Start Menu entry and shortcut. The native package name intentionally has no
spaces; the launcher window and setup prompts display “Leon Bookings”.

The optional `--install-deps` flag is supported on Linux (`dnf`/`apt-get`) and
macOS (Homebrew); it installs only the build prerequisites required by the
chosen package format. Use `--manager` if automatic package-manager detection
doesn't work. Package managers may request administrator approval.
Windows prerequisites are checked by `package.ps1` and are installed
separately as described above.

Each build runs `./mvnw -B package` (or `mvnw.cmd` on Windows, including the
Maven test phase), compiles the small Swing launcher, and creates the package
under `install/dist/`. The first wrapper run downloads Maven 3.9.11 and
verifies its SHA-512 checksum; later runs use the local Maven cache. The `dist`
directory is generated output and is intentionally ignored by Git; this
folder's source, scripts, and instructions are tracked.

The `Desktop installers` GitHub Actions workflow builds the Debian package,
portable Linux archive, macOS DMG, and Windows MSI on matching hosted operating
systems when run manually or when a version tag is pushed. Before building a
tagged release, it verifies the tag matches the application version in
`pom.xml` (for example, `pom.xml` version `1.0.1` requires tag `v1.0.1`). A
matching tag automatically creates a GitHub Release with generated notes and
all four installer files attached. Manual workflow runs only upload workflow
artifacts.

To publish a release, update the project version in `pom.xml`, commit that
change, then create and push the matching tag, for example:

```sh
git tag v1.0.1
git push origin v1.0.1
```

Maven Wrapper's Maven version is a separate build-tool pin; it does not need to
change for an application release.

The application binds to `127.0.0.1:8080`. If that port is already in use, the
launcher reports the startup log location in the user's data directory. On
macOS, a smooth public release still requires Apple signing and notarization.
Windows MSI packaging requires WiX Toolset 3 on `PATH`.
The MSI uses a stable upgrade identifier so newer versions can upgrade the
existing application without removing the user's database.

## API versions

The current frontend uses `/api/v1` routes, including
`/api/v1/booking/...` and `/api/v1/csrf`. Existing `/booking/...` and `/csrf`
routes remain available for compatibility. Changes that break clients should
be introduced under a new major path such as `/api/v2`; do not silently change
the meaning or shape of `/api/v1` responses.
