
# Leon Bookings (Karpathos)

Room and vehicle booking management with a React interface, Spring Boot backend,
and SQLite storage. The UI is available in English, Greek, German, Dutch,
Danish, and French.

## Application overview

Leon Bookings is a self-hosted planner for apartments/studios and cars/bikes
(product branding: Leon Soft). Authenticated users manage reservations on a
timeline, with financial fields (charge, received, commission, balance) and
localized HTML/PDF reports. It is a single-process application, not a
multi-tenant SaaS: user accounts and roles are stored in the local SQLite
database, booking data lives in the same SQLite file, and one deployable Spring
Boot process also serves the React UI.

| Layer | Technology |
|--------|------------|
| Backend | Java 25, Spring Boot 4.1 (MVC, Security, JPA, Validation) |
| Database | SQLite |
| Frontend | React 19 + Vite, i18next, vis-timeline |
| Reports | OpenPDF + localized HTML templates |
| Packaging | Maven fat JAR, Docker Compose, optional desktop launcher (`jpackage`) |

```text
Browser → React UI → /api/v1/* → Controllers → Services → JPA → SQLite
                                      ↘ HTML / PDF reports
```

Main screens use query parameters:

- `/?type=rooms` or `/?type=cars` — booking form and dual timelines (active/paid
  vs cancelled)
- `?page=reports` — filtered reports and PDF export
- `?page=resources` — add rooms and cars to the catalog

Authentication uses database-backed users with `ADMIN`, `OPERATOR`, and `GUEST`
roles. On first startup, default accounts are seeded only when missing; set
`APP_ADMIN_USERNAME/PASSWORD`, `APP_OPERATOR_USERNAME/PASSWORD`, and
`APP_GUEST_USERNAME/PASSWORD` in the environment to control those seed accounts.
Run it with Docker Compose, `./mvnw generate-resources spring-boot:run`, or a
desktop package under `install/`. An offline `db backup|restore` CLI is
available on the JAR before Spring starts.

For entities, API routes, package layout, and current boundaries, see the
[application deep dive](docs/application-deep-dive.md). Broader architecture
notes live in [`docs/`](docs/README.md).

## Quick start for users (Docker)

Install Docker Engine/Desktop with the Docker Compose plugin. From the project
directory, create the local environment file and set unique credentials for the
seeded accounts. Passwords must be at least 12 characters:

```sh
cp .env.example .env
```

Edit `.env` and replace the placeholder passwords with strong, unique values for
the admin, operator, and guest seed accounts. Then build and start the application:

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

LAN access is opt-in for the desktop launcher and Docker Compose. The desktop
launcher listens only on this computer by default; set
`SERVER_ADDRESS=0.0.0.0` before launching to allow LAN devices to connect to
`http://<host-computer-LAN-IP>:8080`. For Docker Compose, set
`BOOKING_HOST_ADDRESS=0.0.0.0` in `.env`. Direct runs with the `prod` profile
bind to loopback by default; set `SERVER_ADDRESS=0.0.0.0` to opt in to remote
access. Enabling LAN access exposes the app on all host network
interfaces: use a strong administrator password and restrict port 8080 with
the host firewall to your trusted LAN. The app does not provide HTTPS itself.

For a desktop portable app, set the variable in the same terminal before
starting the launcher:

```powershell
$env:SERVER_ADDRESS = "0.0.0.0"
.\LeonBookings.exe
```

```sh
# Linux
SERVER_ADDRESS=0.0.0.0 ./bin/LeonBookings

# macOS
SERVER_ADDRESS=0.0.0.0 ./LeonBookings.app/Contents/MacOS/LeonBookings
```

For Docker Compose, set `BOOKING_HOST_ADDRESS=0.0.0.0` in `.env` and run
`docker compose up -d`. To return to local-only access, remove the desktop
environment override or restore `BOOKING_HOST_ADDRESS=127.0.0.1` and restart.

The SQLite database remains on the computer running the application. To share
or move its data, stop the app and use the documented backup/restore commands;
do not have multiple computers open the same live `bookings.db` file from a
network share.

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

The default seed account credentials are read from `APP_ADMIN_*`,
`APP_OPERATOR_*`, and `APP_GUEST_*` environment variables at startup. Missing seed
accounts are created in SQLite; existing user accounts are not overwritten.
Passwords must be at least 12 characters. Protect the environment and avoid putting
real credentials in source control.

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
They include a command-line launcher that prompts for administrator credentials,
starts the local service, and opens the app in the browser. Run the launcher
from a terminal; it keeps the service running until you press Enter or
interrupt it. Each installer bundles only the Java 25 runtime modules needed
to run the app; end users do not need a system Java installation, Maven,
Node.js, npm, or Docker. Installer build scripts check for JDK 25
and native packaging tools before starting; see
[`install/README.md`](install/README.md) for setup instructions for Linux,
macOS, and Windows. On Fedora, Debian-based
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

#### Run the portable Linux archive

The archive includes the application and its Java runtime. From the directory
where you downloaded `LeonBookings-2.0.1-linux-x86_64.tar.gz`, extract and run
it:

```sh
tar -xzf LeonBookings-2.0.1-linux-x86_64.tar.gz
cd LeonBookings
./bin/LeonBookings
```

Replace the filename with the version and architecture you downloaded. The
launcher asks for the administrator credentials, starts the service, and opens
the app in your browser. Keep the terminal open while using the app; pressing
Enter stops the service. The database is `bookings.db` in the launcher's
current working directory. An existing database is retained and receives any
missing tables; a new database is initialized with one sample booking. Logs
are stored under `${XDG_DATA_HOME:-~/.local/share}/leon-bookings`.

#### Portable Windows and macOS apps

The Windows and macOS portable builds are ZIP archives with the application
and Java runtime bundled. Build on the target operating system:

```powershell
# Windows PowerShell
.\install\package.ps1 portable
```

```sh
# macOS
./install/package.sh portable
```

Extract the archive, then start the app from a terminal in its extracted
directory. This keeps `bookings.db` in that directory:

```powershell
cd .\LeonBookings
.\LeonBookings.exe
```

```sh
./LeonBookings.app/Contents/MacOS/LeonBookings
```

The macOS ZIP matches the architecture of the build machine. These portable
archives are not signed or notarized, so Windows SmartScreen or macOS
Gatekeeper may show warnings. The existing Windows MSI and macOS DMG builds
remain available.

## API versioning

The frontend uses the major-versioned `/api/v1` routes, such as
`/api/v1/booking/getAll` and `/api/v1/csrf`. Breaking API changes should use a
new major prefix, such as `/api/v2`, rather than changing v1 behavior.

## Releases

Release tags use the `v<major>.<minor>.<patch>` format and trigger the desktop
installer workflow. See [CHANGELOG.md](CHANGELOG.md) for curated release notes.

## Developer documentation

The [`docs/`](docs/README.md) folder describes the system architecture,
frontend, backend, current tests, and architectural decisions. Start with the
[application deep dive](docs/application-deep-dive.md) for a full product and
codebase walkthrough. Architecture docs also include Mermaid diagrams that
render in GitHub Markdown.

### React frontend

The React/Vite source is in `frontend/`. It shares one booking screen between
rooms and vehicles, uses the existing vis-timeline browser library, and calls
the current Spring booking endpoints. Access is protected by database-backed
user accounts with `ADMIN`, `OPERATOR`, and `GUEST` roles. Default seed accounts
can be configured through `APP_ADMIN_*`, `APP_OPERATOR_*`, and `APP_GUEST_*`
environment variables; existing accounts are not overwritten on startup.
Active/paid bookings and cancelled bookings appear in separate timeline panels.
Cancelled entries are stacked in their panel so overlapping cancellations,
including multiple records for the same guest, remain individually visible.
Overlap prevention for active/paid reservations is enabled separately for
rooms and vehicles by default. Override either global application setting
with `BOOKING_AVAILABILITY_ROOMS_PREVENT_OVERLAP` or
`BOOKING_AVAILABILITY_CARS_PREVENT_OVERLAP`; these are deployment environment
settings, not switches in the application UI. When enabled, overlapping
active/paid bookings for the same resource are rejected, while cancelled
bookings do not block availability.
Administrators can add rooms and vehicles from the **Manage rooms and cars**
screen. Existing bookings keep their stored resource assignments.

#### Recent security and usability improvements

- Booking create, update, and delete requests use validated request objects.
  The backend validates required values, dates, status, email, and financial
  amounts, then calculates nights and balance itself.
- Authentication uses database-backed users with `ADMIN`, `OPERATOR`, and `GUEST`
  roles. Admins manage users, resources, and admin ops and can view reports; operators manage bookings and view reports; guests have read-only booking/resource visibility. Browser writes include CSRF protection. The
  `booking_resources` table stores the managed room and vehicle catalog; the
  existing bookings table and its resource assignments are preserved.
- The detailed HTML report escapes booking data before rendering. The SPA
  fallback reads the entry page from the classpath, and static assets are
  configured for packaged applications.
- Development and desktop application runs bind to `127.0.0.1` by default.
  Docker Compose publishes the port on loopback by default. Optional LAN
  access is documented above and in the in-app Help.
- Booking and report screens support responsive layouts, keyboard-accessible
  report tables, and a persistent light/dark theme preference.

Open `/?type=rooms&page=reports` or `/?type=cars&page=reports` for the reports
page. It summarizes bookings from the database-backed
`GET /api/v1/booking/getAll` endpoint, filters by arrival date and resource
type, and provides detailed HTML and PDF exports using the versioned report
endpoints. The PDF export follows the selected type and arrival-date range and
includes the on-screen totals, per-resource breakdown, and booking columns.

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

The booking and reports interfaces support English, Greek, German, Dutch,
Danish, and French. Use the language selector in the header; the choice is
saved in the current browser. English is the default and fallback. Dates and
currency follow that language's locale, and detailed reports use the selected
language. The reports screen's Export PDF action exports the selected resource
type and date range, including the displayed summaries and resource breakdown.
The sign-in page offers the same languages. Booking values and API
status codes remain language-neutral. Frontend translation catalogs are in
`frontend/src/locales/`; the build validates catalog keys and interpolation
placeholders.

In edit mode, the frontend uses `PUT /api/v1/booking/update`. When user
management is added, a user's preference can override this global frontend
default.

### SQLite backup and restore CLI

The packaged application includes an offline Java CLI and uses the existing
SQLite JDBC dependency; Python is not required. Run it with the same Java 25
runtime as the application. The database path defaults to `bookings.db` in the
current working directory, matching the application's relative SQLite URL.
Pass `--database` if the application uses another path.

Before installing an update, stop the application and create an encrypted
backup outside the application folder. Keep the existing database at the same
path and check the path printed at startup; after updating, confirm that recent
bookings and resources are present before removing the previous application
copy. Current SQL initialization creates missing tables but does not migrate
changed columns, so test updates against a copy of the customer database and
keep the backup until the update is verified.

Create a timestamped backup in `backups/`:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db backup
```

The plain backup command creates an unencrypted file; do not email or otherwise
share that file. For a support copy, create an encrypted backup instead. The command prompts
for a passphrase twice without echoing it and writes a timestamped
`*.sqlite3.enc` file:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db backup --encrypt
```

Choose a long, unique passphrase (at least 12 characters). The backup uses
AES-256-GCM with a random salt and nonce. Keep the passphrase separate from the
backup file and share it with support through a different trusted channel.
There is no passphrase recovery; losing it makes the backup unusable. The app
does not email or upload backups. Send the encrypted file through your approved
support channel and communicate the passphrase separately. The application
detects encrypted backups automatically during restore and prompts for the
passphrase. Restore only after stopping the application.

Choose a destination or database explicitly:

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db backup \
  --database /path/to/bookings.db \
  --output /path/to/backup.sqlite3
```

The same options work with `--encrypt`; use an output filename ending in
`.sqlite3.enc`.

Restore a backup after stopping the application. The CLI asks for confirmation;
type `RESTORE` to continue. Before replacing an existing database, it validates
the restore file and writes a timestamped pre-restore backup to `backups/`.
Encrypted backups are detected automatically; the CLI asks for their
passphrase before validating and restoring them.

```sh
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
java -jar "target/leonbookinsKarpathos-${VERSION}.jar" db restore \
  --input backups/bookings-YYYYMMDD-HHMMSS.sqlite3.enc
```

Use the `.sqlite3` filename for an unencrypted backup.

For an unencrypted backup, explicitly confirm with `--yes` and select a backup
directory if needed:

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
you need to undo it. Encrypted restore requires an interactive terminal for
passphrase entry even when `--yes` is used. Keep all backup files in a secure
location because they contain booking data.

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
report HTML escaping and paginated PDF contents, authentication/authorization/
CSRF behavior, and SQLite backup/restore safety. These tests do not replace a
full browser/device usability check.

### Docker details

The production image is built in two stages: Maven packages the application
and frontend, then a Java 25 runtime image runs the app as a non-root user.
Compose stores SQLite data in a named volume and configures the app to listen
inside the container. For remote access, configure a trusted HTTPS reverse
proxy rather than exposing the application directly to the internet.

Seed account passwords must be at least 12 characters. Browser sessions use CSRF protection for write requests.

| Capability | ADMIN | OPERATOR | GUEST |
|---|:---:|:---:|:---:|
| View all bookings | Yes | Yes | Yes |
| Create/edit/delete bookings | Yes | Yes | No |
| View reports / detailed report / export PDF | Yes | Yes | No |
| Export CSV (Reports page, client-side) | Yes | Yes | No |
| View rooms/cars resources | Yes | Yes | Yes |
| Create/edit/manage rooms & cars | Yes | No | No |
| Manage users | Yes | No | No |
| View app version/info | Yes | Yes | Yes |
| Support | Yes | Yes | Yes |

The system prevents removing, deactivating, or deleting the last active ADMIN or last active OPERATOR account.

For Vite hot-reload development, install Node.js/npm and run `npm ci` followed
by `npm run dev` from `frontend/`, with Spring Boot running on port 8080.
Export the seed account environment variables before starting Spring Boot.
The build writes the bundled React app to the ignored
`src/main/resources/public/react-assets/` directory. `/rooms.html` and
`/cars.html` remain as redirects to the shared screen.

## Compliance scope (local only) and unsupported items

> **Not legal advice.** This app is a local booking manager, not a compliance platform. It does **not** make external regulatory/API calls. Requirements vary by country, municipality, property type, and role (host/manager/platform).

### Currently supported locally

- **Resource compliance fields (manual):** registration number, property address, tax category, per-night rental tax rate.
- **Booking compliance fields:** platform reservation ID, guest identity/contact fields already present (name, country, ID/passport, AFM, email, phone, voucher, tax/invoice refs).
- **Tax calculation (manual):** per-resource daily rental tax rate; booking stores rental tax amount snapshot and balance = charge − received − commission − rental tax.
- **Local reports/exports:** HTML/PDF report and client-side CSV export include compliance/financial columns for local record keeping.

### Not supported currently

| Area | Not supported | Notes |
|---|---|---|
| EU 2024/1028 STR data collection/sharing | No unit registration workflow integration, no registration-number verification, no single digital entry point submission, no platform activity-data transmission | App stores registration number locally only. |
| DAC7 platform reporting | No DAC7 seller/host report generation/submission, no platform-grade seller identity/TIN/DOB/company-reg validation, no quarterly payout/withholding reporting package | Relevant mainly to platforms; app is not an OTA reporting platform. |
| Greece TAKK / climate resilience fee automation | No seasonal/category automatic rate engine, no official TAKK category mapping, no AADE declaration/export/submission, no exemption handling | Rental tax is manual per resource and snapshot on booking. |
| Greek STR registration/operational compliance | No AMA/MHTE registry integration, no property permit/licence workflow, no legal display/compliance checks | Registration number can be stored manually. |
| Guest identity/compliance data | No structured DOB/nationality/document type/expiry/country-of-residence capture, no police/guest-register submission, no ID verification | Only basic ID/passport and contact fields exist. |
| Tax/invoicing compliance | No VAT/tax breakdown, no tax jurisdiction rules, no e-invoicing/myDATA/AADE integration, no immutable fiscal ledger | Invoice/tax reference fields are plain text. |
| Audit/GDPR compliance tooling | No immutable compliance audit log, no consent/legal-basis tracking, no retention/deletion/anonymization workflows, no subject export/erasure tooling | Handle personal data carefully and define local operational procedures. |
| External integrations | No external API calls, no OTA sync, no government/registry connections, no automated compliance submissions | Intentional by design for local/offline operation. |
