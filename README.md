
# Leon Bookings (Karpathos)

Room and vehicle booking management with a React interface, Spring Boot backend,
and SQLite storage. The UI is available in English and Greek.

## Quick start for users (Docker)

Install Docker Engine/Desktop with the Docker Compose plugin. From the project
directory, create the local environment file and set a unique administrator
username and password. The password must be 12–72 UTF-8 bytes:

```sh
cp .env.example .env
```

Edit `.env` and replace `CHANGE_ME` with a private password, then build and
start the application:

```sh
docker compose up --build -d
```

Open [http://localhost:8080](http://localhost:8080) and sign in with the
credentials from `.env`. The SQLite database is stored in the persistent
`booking-data` Docker volume and survives container upgrades and restarts. The
published port is bound to this computer only by default. Docker starts with a
new database; it does not automatically import a repository-local
`bookings.db`.

To stop the application, run `docker compose down`. This keeps the database.
To permanently remove the database too, run `docker compose down --volumes`.
Keep `.env` private and make regular database backups.

The project currently provides a Docker build, not a published container
registry image; the first start builds the image locally.

Docker Compose explicitly activates the Spring `prod` profile. Direct
development runs use the `dev` profile by default; override it with
`SPRING_PROFILES_ACTIVE=prod` for a production deployment. The `dev` profile
binds to loopback, enables more application logging, and disables static
resource caching. The `prod` profile listens on all interfaces by default
(override with `SERVER_ADDRESS`), uses normal application log levels, suppresses
stack traces in error responses, and only exposes the Actuator health endpoint
if Actuator is enabled.

## Developer setup (Maven)

Install JDK 25. The checked-in Maven Wrapper downloads the project's pinned
Maven 3.9.11 distribution on first use and verifies its SHA-512 checksum.
Internet access is required for that initial download. Node.js/npm are not
required for the Maven build: the build downloads pinned Node.js/npm versions,
installs the frontend dependencies from the lockfile, and builds the React app.

Set the administrator credentials in the environment before running the
application. Example for Linux/macOS:

```sh
export APP_ADMIN_USERNAME=admin
read -rsp "Admin password: " APP_ADMIN_PASSWORD && export APP_ADMIN_PASSWORD && echo
```

In PowerShell:

```powershell
$env:APP_ADMIN_USERNAME = "admin"
$env:APP_ADMIN_PASSWORD = Read-Host "Admin password"
```

The username and password are read from `APP_ADMIN_USERNAME` and
`APP_ADMIN_PASSWORD` at startup. The password must be 12–72 UTF-8 bytes. This
application currently has one in-memory administrator account; it does not
store user accounts or passwords in SQLite. Protect the environment and avoid
putting real credentials in source control.

Build and run the tests:

```sh
./mvnw clean test
./mvnw package
```

In PowerShell, use `.\mvnw.cmd` instead of `./mvnw`.

Generate the Maven project site with `./mvnw site`; the output is written to
`target/site/` (use `.\mvnw.cmd site` in PowerShell).

Run the app directly from Maven (including the frontend build):

```sh
./mvnw generate-resources spring-boot:run
```

The default `dev` profile binds to `127.0.0.1:8080` and uses `bookings.db` in
the working directory by default. Override the database JDBC URL with
`APP_DB_URL`, for example `jdbc:sqlite:/path/to/bookings.db`. To run with
production settings directly, set `SPRING_PROFILES_ACTIVE=prod` before launch.

Run the frontend unit tests independently with Node.js 22 or newer:

```sh
cd frontend
npm ci
npm test
```

## Desktop installers

Self-contained desktop installers and their build scripts live in `install/`.
They include a small launcher that prompts for administrator credentials,
starts the local service, and opens the app in the browser. The installer
bundles its Java runtime; end users do not need Java, Maven, Node.js, npm, or
Docker. Installer build scripts check for JDK 25 and native
packaging tools before starting; see [`install/README.md`](install/README.md)
for setup instructions for Linux, macOS, and Windows. On Fedora, Debian-based
Linux, and macOS, `./install/package.sh --install-deps` can install missing
build prerequisites through the system package manager. If Linux detection
fails, pass the manager explicitly, for example
`./install/package.sh --install-deps --manager dnf`; Fedora selects the
portable bundled-runtime archive automatically.
The JDK must include its `jmods` directory so `jpackage` can build the
self-contained runtime; the installer preflight checks for it.

Build each native package on its matching operating system; see
[`install/README.md`](install/README.md) for requirements and output locations.
macOS releases need signing and notarization for a smooth first launch.

## API versioning

The frontend uses the major-versioned `/api/v1` routes, such as
`/api/v1/booking/getAll` and `/api/v1/csrf`. Existing `/booking/...` and
`/csrf` routes remain available for older clients. Breaking API changes should
use a new major prefix, such as `/api/v2`, rather than changing v1 behavior.

## Developer documentation

The [`docs/`](docs/README.md) folder describes the system architecture,
frontend, backend, current tests, and architectural decisions. It includes
Mermaid architecture and class diagrams that render in GitHub Markdown.

### React frontend

The React/Vite source is in `frontend/`. It shares one booking screen between
rooms and vehicles, uses the existing vis-timeline browser library, and calls
the current Spring booking endpoints. Access is protected by one administrator
account configured with `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`.
For direct and Docker runs, credentials come from process environment
variables. The desktop launcher asks for the username and password on every
launch and passes them to the application process; it does not save them to
the database or configuration files.
Active/paid bookings and cancelled bookings appear in separate timeline panels.
The administrator can add rooms and vehicles from the **Manage rooms and cars**
screen. Existing bookings keep their stored resource assignments.

#### Recent security and usability improvements

- Booking create, update, and delete requests use validated request objects.
  The backend validates required values, dates, status, email, and financial
  amounts, then calculates nights and balance itself.
- Admin-only booking and report endpoints use a single in-memory administrator
  account. Browser writes include CSRF protection; no user table or database
  schema changes were added.
- The detailed HTML report escapes booking data before rendering. The SPA
  fallback reads the entry page from the classpath, and static assets are
  configured for packaged applications.
- Local application runs bind to `127.0.0.1`. For Docker, the documented
  command overrides the bind address inside the container.
- Booking and report screens support responsive layouts, keyboard-accessible
  report tables, and a persistent light/dark theme preference.

Open `/?type=rooms&page=reports` or `/?type=cars&page=reports` for the reports
page. It summarizes bookings from the database-backed
`GET /api/v1/booking/getAll` endpoint, filters by arrival date and resource
type, and provides detailed HTML and PDF exports using the versioned report
endpoints.

The global frontend save behavior defaults to creating a new booking when
`VITE_BOOKING_SAVE_MODE` is unset. Set it to `edit` at build time to default to
updating a selected booking instead. A frontend checkbox can switch between
create and edit modes at runtime for testing; its choice lasts for the current
browser tab session. With edit mode enabled, submitting an unselected form
still creates a new booking. This configuration is frontend-only and is
centralized in `frontend/src/appConfig.js`. For example:

```sh
VITE_BOOKING_SAVE_MODE=edit ./mvnw package
```

The interface supports light and dark themes. Use the theme button in the
header to switch; the preference is saved in the current browser.

The booking and reports interfaces support English and Greek. Use the language
selector in the header; the choice is saved in the current browser. Dates and
currency follow that language's locale, and HTML/PDF reports use the selected
language. The sign-in page offers the same languages. Booking values and API
status codes remain language-neutral.

In edit mode, the frontend uses `PUT /api/v1/booking/update`. When user
management is added, a user's preference can override this global frontend
default.

### SQLite backup and restore CLI

The packaged application includes an offline Java CLI and uses the existing
SQLite JDBC dependency; Python is not required. Run it with the same Java 25
runtime as the application. The database path defaults to `bookings.db` in the
current working directory, matching the application's relative SQLite URL.
Pass `--database` if the application uses another path.

Create a timestamped backup in `backups/`:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db backup
```

Choose a destination or database explicitly:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db backup \
  --database /path/to/bookings.db \
  --output /path/to/backup.sqlite3
```

Restore a backup after stopping the application. The CLI asks for confirmation;
type `RESTORE` to continue. Before replacing an existing database, it validates
the restore file and writes a timestamped pre-restore backup to `backups/`.

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db restore \
  --input backups/bookings-YYYYMMDD-HHMMSS.sqlite3
```

For automation, explicitly confirm with `--yes` and select a backup directory
if needed:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db restore \
  --database /path/to/bookings.db \
  --input /path/to/backup.sqlite3 --backup-dir /path/to/backups --yes
```

Both operations check SQLite integrity and require the application's `Booking`
table. Backup creation uses SQLite's online backup API and refuses to
overwrite an existing output file. Restore replaces the target only after
validating a staged copy. If SQLite WAL, shared-memory, or journal sidecars
remain beside the database, restore stops rather than risk discarding pending
data; resolve the sidecars before retrying. Restore the pre-restore backup if
you need to undo it. Keep backup files in a secure location because they
contain booking data.

### Tests

Run the backend tests with Maven and Java 25:

```sh
./mvnw test
```

Run the frontend unit tests from `frontend/`:

```sh
cd frontend
npm ci
npm test
```

Frontend tests cover booking/date logic, timeline filtering, reports, and theme
preferences. Backend tests cover request validation, booking service behavior,
report HTML escaping, authentication/authorization/CSRF behavior, and SQLite
backup/restore safety. These tests do not replace a full browser/device
usability check.

### Docker details

The production image is built in two stages: Maven packages the application
and frontend, then a Java 25 runtime image runs the app as a non-root user.
Compose stores SQLite data in a named volume and configures the app to listen
inside the container. For remote access, configure a trusted HTTPS reverse
proxy rather than exposing the application directly to the internet.

Administrator passwords must be 12–72 UTF-8 bytes. Booking and report
endpoints require the administrator role; browser sessions use CSRF protection
for write requests.

For Vite hot-reload development, install Node.js/npm and run `npm ci` followed
by `npm run dev` from `frontend/`, with Spring Boot running on port 8080.
Export the same administrator environment variables before starting Spring Boot.
The build writes the bundled React app to the ignored
`src/main/resources/public/react-assets/` directory. `/rooms.html` and
`/cars.html` remain as redirects to the shared screen.
