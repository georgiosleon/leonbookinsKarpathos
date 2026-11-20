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
`SPRING_PROFILES_ACTIVE=prod`; it binds to all interfaces by default, keeps
stack traces out of HTTP error responses, and limits Actuator exposure to
health. Docker Compose activates `prod` automatically. Set `SERVER_ADDRESS` to
override the production bind address.

The SQL initializer remains enabled in both profiles because the current
`schema.sql` uses idempotent table/index creation and seed inserts.

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
| `GET` | `/api/v1/booking/report` | Generate a localized report response. |
| `GET` | `/api/v1/booking/export/pdf` | Download a localized PDF report. |
| `GET` | `/api/v1/csrf` | Obtain the CSRF token for browser writes. |
| `GET` | `/api/v1/resources` | List managed rooms and vehicles. |
| `POST` | `/api/v1/resources` | Add a room or vehicle using a JSON `type` and `name`. |

Report requests accept the date range and optional language parameters shown
in the controller. Check the controller and frontend API module for exact
parameter formats before adding clients. Introduce breaking API changes under
a new major prefix (for example `/api/v2`).

## Security

The current configuration uses one administrator identity supplied through
`APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`, with administrator-only booking
and report access. Browser sessions use CSRF protection for state-changing
requests. This is not yet a persisted user directory, invitation flow, or
configurable per-user permission system.

Do not put credentials in frontend source or commit real secrets. Local
application runs bind to loopback by default; deployment configuration should
preserve that boundary or place remote access behind an appropriately
configured trusted HTTPS proxy.

## Persistence and reports

`Booking` is the JPA entity and `BookingRepository` provides persistence
operations. `DBConfig` configures SQLite. The active database path depends on
the launch mode and configuration; use `APP_DB_URL` to select a JDBC URL when
running locally.

`BookingResource` and `BookingResourceRepository` manage the room/car catalog
in the `booking_resources` table. `schema.sql` creates that table idempotently
and seeds the resource IDs already used by existing bookings; it does not
rewrite the existing booking table. A booking continues to store the resource
ID in its existing `room` column, preserving current booking data.

`BookingsService` coordinates booking operations and report data. Report
rendering uses `BookinsHtmlTemplate` and `ReportMessages`; the controller
exposes report text and PDF responses. Report HTML must continue escaping
booking data before rendering.

The `db backup` and `db restore` CLI validates SQLite integrity and the
application booking table. Restore is an offline operation and should be
performed after stopping the application.

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
