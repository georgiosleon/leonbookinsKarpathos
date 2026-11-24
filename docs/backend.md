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

Schema changes are managed by Flyway SQL migrations in
`src/main/resources/db/migration` with `spring.jpa.hibernate.ddl-auto=validate`.
`DatabaseInitializer` seeds default accounts only when missing. Back up existing
customer databases before upgrading and test releases against copies of real data.

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
for compatibility. Access is deny-by-default; endpoints below require authentication unless noted.

| Method | Route | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/booking/getAll` | ADMIN, OPERATOR, GUEST | List bookings (all authenticated roles see all bookings). |
| `POST` | `/api/v1/booking/save` | ADMIN, OPERATOR | Create a booking. |
| `PUT` | `/api/v1/booking/update` | ADMIN, OPERATOR | Update any booking. |
| `POST` | `/api/v1/booking/delete` | ADMIN, OPERATOR | Delete a booking by ID. |
| `POST` | `/api/v1/booking/del?bid=...` | ADMIN, OPERATOR | Delete a cancelled booking only (legacy route; CSRF required). |
| `GET` | `/api/v1/booking/report` | ADMIN, OPERATOR | Generate a localized detailed report response. |
| `GET` | `/api/v1/booking/export/pdf` | ADMIN, OPERATOR | Download filtered PDF report. |
| `GET` | `/api/v1/settings/availability` | ADMIN, OPERATOR, GUEST | Read room/vehicle overlap rules. |
| `GET` | `/api/v1/resources` | ADMIN, OPERATOR, GUEST | List managed rooms and vehicles. |
| `POST` | `/api/v1/resources` | ADMIN | Add a room or vehicle. |
| `PUT` | `/api/v1/resources/{id}` | ADMIN | Update a room or vehicle. |
| `GET` | `/api/v1/users/me` | Authenticated | Current user identity/role for UI gating. |
| `GET/POST` | `/api/v1/users` | ADMIN | List/create users. |
| `GET/PUT/DELETE` | `/api/v1/users/{id}` | ADMIN | Get/update/delete user. |
| `GET` | `/api/v1/license/status` | Authenticated | License activation status and installation ID. |
| `POST` | `/api/v1/license/activate` | ADMIN | Activate installation with a signed license key. |
| `GET/POST` | `/api/v1/customers` | ADMIN | List/create licensed customer records (one per installation). |
| `PUT/DELETE` | `/api/v1/customers/{id}` | ADMIN | Update/delete customer record. |
| `GET` | `/api/v1/app-info` | Authenticated | Application version/deployment/license info. |
| `GET` | `/api/v1/diagnostics` | Authenticated | Diagnostics download. |
| `GET` | `/api/v1/csrf` | Authenticated | Obtain CSRF token for browser writes. |

Report requests accept `fromDate` and `toDate` in `dd/MM/yyyy` format and an
optional `lang` language code. PDF requests also require `type=rooms` or
`type=cars`. Check the controller and frontend API module for exact parameter
formats before adding clients. Introduce breaking API changes under a new
major prefix (for example `/api/v2`).

## Security

Authentication uses database-backed users in the `users` table with roles
`ADMIN`, `OPERATOR`, and `GUEST`. Spring Security enforces role-based access and CSRF protection for
browser state-changing requests.

Default seed accounts are created only when missing, using `APP_ADMIN_*`,
`APP_OPERATOR_*`, and `APP_GUEST_*` environment
variables. Seed passwords must be at least 12 characters; built-in default password `1qaz2wsx3edc` (and legacy `change-me-*` values) is rejected in production.

Role matrix:

| Capability | ADMIN | OPERATOR | GUEST |
|---|:---:|:---:|:---:|
| View all bookings | Yes | Yes | Yes |
| Create/edit/delete bookings | Yes | Yes | No |
| View reports / detailed report / export PDF | Yes | Yes | No |
| View rooms/cars resources | Yes | Yes | Yes |
| Create/edit/manage rooms & cars | Yes | No | No |
| Manage users | Yes | No | No |
| Activate/manage license & customer | Yes | No | No |
| View app version/info | Yes | Yes | Yes |
| Support | Yes | Yes | Yes |

The system prevents removing, deactivating, or deleting the last active admin or
operator account. Do not commit real secrets. LAN/internet exposure requires
trusted-network controls and HTTPS proxying; the app does not provide TLS.

## Licensing

Licensing is offline and one-time per installation. `app_installation` stores a
generated `install_id`; `customer` stores the single licensed customer record;
`app_license` stores the signed license key and activation metadata. Admins
activate by pasting an Ed25519-signed key (`base64url(payload).base64url(signature)`)
through `POST /api/v1/license/activate`. `LicenseEnforcementFilter` blocks normal
API use until activated, while allowing login/static/SPA shell, CSRF, current-user,
license status, and admin license/customer endpoints. Activation takes effect
immediately with no restart. For production, configure `APP_LICENSE_PUBLIC_KEY`
(base64 X.509 Ed25519 public key); the dev key is used only as fallback.

## Persistence and reports

`Booking` is the JPA entity and `BookingRepository` provides persistence
operations. `DBConfig` configures SQLite. The active database path depends on
the launch mode and configuration; use `APP_DB_URL` to select a JDBC URL when
running locally.

`BookingResource` and `BookingResourceRepository` manage the room/car catalog
in the `booking_resources` table. `User` and `UserRepository` manage
database-backed user accounts. Bookings store the stable resource ID in
`resourceId` (`resource_id` column) and expose a display `resourceName`.

Schema changes are managed with Flyway migrations (`ddl-auto=validate`).
`DatabaseInitializer` seeds default accounts when missing. Back up existing
customer databases before upgrading and test releases against copies.

For LAN use, clients connect to the host running the app; the SQLite database
stays on that host. Do not point multiple application instances at the same
live SQLite database over a network share. Use the backup/restore CLI to move
or exchange a database while the app is stopped.

`BookingsService` coordinates booking operations and report data. Report
rendering uses `BookingsHtmlTemplate` and `ReportMessages`; the controller
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
