# Backend

## Technology and startup

The backend is a Spring Boot application using Java 25, Spring MVC, Spring
Security, Jakarta Bean Validation, Spring Data JPA, and SQLite. The main entry
point is `com.leonsoft.Application`.

The Maven lifecycle builds the React app and packages it with the Spring Boot
application. The same application can run directly, in Docker, or behind the
desktop launcher. Running `java -jar ... db backup|restore ...` takes the
offline database CLI path instead of starting the web server.

## Spring profiles

Shared configuration is in `application.properties`. When no profile is
specified, Spring uses `dev`: the server binds to loopback, application logging
is more detailed, and static resource caching is disabled. Production
deployments should explicitly activate `prod` with
`SPRING_PROFILES_ACTIVE=prod`; direct runs bind to loopback by default, keep
stack traces out of HTTP error responses, and limits Actuator exposure to
health. Docker Compose activates `prod` automatically and overrides the bind
address inside the container so its published port can reach the app. Set
`SERVER_ADDRESS=0.0.0.0` to explicitly enable remote access to a direct run.
The desktop launcher defaults to
`127.0.0.1` and honors `SERVER_ADDRESS` when it is set. To enable LAN access
from the desktop package, set it to `0.0.0.0` before launching.

Docker Compose keeps its published port on `127.0.0.1` by default. Set
`BOOKING_HOST_ADDRESS=0.0.0.0` in `.env` to publish port 8080 on all host
interfaces. Keep the default unless LAN access is needed. When enabled, use a
strong administrator password and restrict port 8080 to the trusted LAN with
the host firewall; this application does not provide TLS itself.
Direct runs using the `prod` profile bind to `127.0.0.1` by default. Set
`SERVER_ADDRESS=0.0.0.0` only when remote access is intentional, and protect
the application with a trusted HTTPS proxy or trusted-network firewall because
the application does not provide TLS itself.

The SQL initializer remains enabled in both profiles. `schema.sql` creates
the `Booking` and `booking_resources` tables only if absent, and seeds legacy
resource IDs idempotently; it does not overwrite existing booking data.
The desktop launcher selects `bookings.db` from its current working directory;
only when that file is absent does it request `demo-data.sql` for one sample
booking. This is the current bootstrap approach; adopt versioned Liquibase
migrations before making future schema changes that alter existing tables.

## Layers

| Layer | Current responsibility |
|---|---|
| `api` | REST controllers and request DTOs; validates input and maps API data into backend types. |
| `services` | Booking operations, booking-derived calculations, and report generation. |
| `repositories` | Spring Data access to persisted booking entities. |
| `models` | JPA `Booking` entity. |
| `config` | SQLite datasource, Spring MVC/static-resource behavior, and Spring Security. |
| `cli` | Offline SQLite backup and restore operations. |

The API request DTO (`BookingRequest`) is separate from the JPA entity. Keep
validation at the request boundary and business rules in the service layer;
avoid exposing persistence details as new API contracts.

## HTTP API

The frontend uses `/api/v1`; `/booking/...` and `/csrf` aliases are retained
for compatibility. The main routes include:

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/booking/getAll` | List bookings. |
| `POST` | `/api/v1/booking/save` | Create a booking from a validated JSON request. |
| `PUT` | `/api/v1/booking/update` | Update a booking from a validated JSON request. |
| `POST` | `/api/v1/booking/delete` | Delete a booking by ID. |
| `POST` | `/api/v1/booking/del?bid=...` | Delete a cancelled booking only (legacy route; CSRF token required). |
| `GET` | `/api/v1/booking/report` | Generate a localized detailed report response. |
| `GET` | `/api/v1/booking/export/pdf` | Download the filtered room or vehicle report, summary, and resource breakdown as PDF. |
| `GET` | `/api/v1/settings/availability` | Read room and vehicle overlap rules. |
| `GET` | `/api/v1/csrf` | Obtain the CSRF token for browser writes. |
| `GET` | `/api/v1/resources` | List managed rooms and vehicles. |
| `POST` | `/api/v1/resources` | Add a room or vehicle using a JSON `type` and `name`. |

Report requests accept `fromDate` and `toDate` in `dd/MM/yyyy` format and an
optional `lang` language code. PDF requests also require `type=rooms` or
`type=cars`. Check the controller and frontend API module for exact parameter
formats before adding clients. Introduce breaking API changes under a new
major prefix (for example `/api/v2`).

## Security

The current configuration uses one administrator identity supplied through
`APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`, with administrator-only booking
and report access. Browser sessions use CSRF protection for state-changing
requests. This is not yet a persisted user directory, invitation flow, or
configurable per-user permission system.

Do not put credentials in frontend source or commit real secrets. Development
and desktop runs bind to loopback by default; Docker Compose also publishes
the host port on loopback. LAN access is an explicit opt-in documented above.
Do not expose the app directly to the internet; use a trusted HTTPS proxy for
remote access.

## Persistence and reports

`Booking` is the JPA entity and `BookingRepository` provides persistence
operations. `DBConfig` configures SQLite. The active database path depends on
the launch mode and configuration; use `APP_DB_URL` to select a JDBC URL when
running locally.

`BookingResource` and `BookingResourceRepository` manage the room/car catalog
in the `booking_resources` table. `schema.sql` creates both tables idempotently
and seeds the resource IDs already used by existing bookings; existing booking
rows remain unchanged. A booking continues to store the resource ID in its
existing `room` column.

Database initialization is currently idempotent SQL, not a versioned migration
system. It creates missing tables and catalog entries, but does not alter
existing table columns. Back up existing customer databases before upgrading
and test releases against copies; Liquibase or another migration system is a
separate future decision.

For LAN use, clients connect to the host running the app; the SQLite database
stays on that host. Do not point multiple application instances at the same
live SQLite database over a network share. Use the backup/restore CLI to move
or exchange a database while the app is stopped.

`BookingsService` coordinates booking operations and report data. Report
rendering uses `BookinsHtmlTemplate` and `ReportMessages`; the controller
exposes the detailed report text and PDF responses. Report HTML must continue
escaping booking data before rendering. Reports support English, Greek,
German, Dutch, Danish, and French, with English as the fallback. Report text
bundles are stored in `src/main/resources/reports*.properties`. The PDF export
uses the selected resource type and date range and includes the same summary,
resource breakdown, and visible booking columns as the reports screen. It
embeds the licensed DejaVu Sans font with Unicode encoding so Greek text and
punctuation render correctly without relying on fonts installed on the host.

Room and vehicle overlap prevention are independent deployment-level settings,
not currently editable in the application UI. Both default to enabled and can
be overridden with
`BOOKING_AVAILABILITY_ROOMS_PREVENT_OVERLAP` and
`BOOKING_AVAILABILITY_CARS_PREVENT_OVERLAP`. When enabled, overlapping active
or paid bookings for the same resource are rejected by the backend; cancelled
bookings do not block availability. `GET /api/v1/settings/availability`
returns the configured values to the frontend. Changes to environment
configuration take effect after application restart and require no database
schema changes.

The `db backup` and `db restore` CLI validates SQLite integrity and the
application booking table. Restore is an offline operation and should be
performed after stopping the application. `db backup --encrypt` creates an
AES-256-GCM encrypted backup using a passphrase entered interactively; the
restore command detects this format and prompts for the passphrase. The
passphrase is not stored, sent, or recoverable. Keep it separate from the
backup file when sharing a support copy.

## Adding backend features

1. Define or update a request DTO and validation rules for externally supplied
   data.
2. Put domain rules and derived-value calculations in the service layer.
3. Keep repository access in the service/data boundary and persist through the
   JPA entity.
4. Enforce authentication, authority, and CSRF expectations at the Spring
   Security boundary.
5. Add focused tests for validation, service behavior, persistence, and
   authorization as appropriate.
6. Update this API documentation and preserve `/api/v1` compatibility.
