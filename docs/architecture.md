# Architecture

## System overview

```mermaid
flowchart LR
    Browser["Browser"]
    React["React UI<br/>Vite build"]
    API["Spring Boot<br/>REST + security"]
    Service["Booking and report services"]
    Repo["Spring Data repository"]
    ResourceRepo["Resource catalog repository"]
    SQLite[("SQLite database")]
    Reports["HTML / PDF reports"]
    Launcher["Desktop launcher"]

    Browser --> React
    React -->|"same-origin HTTP/JSON"| API
    API --> Service
    Service --> Repo
    Repo --> SQLite
    ResourceRepo --> SQLite
    Service --> Reports
    Launcher -->|"starts local app and opens browser"| API
```

In production, the frontend build is packaged as static resources in the
Spring Boot application. The desktop launcher is an optional wrapper around
that application; Docker and direct Maven runs start the same backend without
the desktop wrapper. SQLite is the application database, with its location
selected by the runtime configuration.

## Request and persistence flow

1. The React screen loads bookings through the frontend API module.
2. Spring Security authenticates the administrator session and applies CSRF
   protection to browser writes.
3. The REST controller validates request DTOs and delegates booking operations
   to `BookingsService`.
4. The service applies booking rules and calls `BookingRepository`.
5. Spring Data JPA persists the `Booking` entity in SQLite.
6. Report requests use the same service/data path and return report text or a
   PDF response.

The database backup/restore CLI is an offline path. `Application` dispatches
the `db` command to `DatabaseCli` before starting Spring Boot.

## Main backend class relationships

```mermaid
classDiagram
    class BookingsRestController {
        +getAll()
        +create(BookingRequest)
        +update(BookingRequest)
        +deleteObject(BookingDeleteRequest)
        +report(fromDate, toDate, language)
        +exportToPDF(fromDate, toDate, language)
    }
    class BookingRequest {
        +toBooking() Booking
    }
    class BookingDeleteRequest
    class BookingsService {
        +getAll()
        +create(Booking)
        +update(Booking)
        +deleteObject(id)
        +report(...)
        +exportToPDF(...)
    }
    class BookingRepository
    class BookingResourceRepository
    class BookingResourceService
    class BookingResourceController
    class BookingResource {
        <<JPA entity>>
    }
    class Booking {
        <<JPA entity>>
    }
    class BookinsHtmlTemplate
    class ReportMessages
    class SecurityConfig
    class DBConfig
    class SQLite

    BookingsRestController --> BookingRequest : validates/maps
    BookingsRestController --> BookingDeleteRequest : validates
    BookingsRestController --> BookingsService : delegates
    BookingsService --> BookingRepository : queries and writes
    BookingRepository --> Booking : persists
    BookingResourceRepository --> BookingResource : persists catalog
    BookingsService --> BookingResourceService : validates assignments
    BookingResourceController --> BookingResourceService : manages catalog
    BookingsService --> BookinsHtmlTemplate : renders HTML
    BookingsService --> ReportMessages : localizes reports
    DBConfig --> SQLite : configures datasource
    SecurityConfig ..> BookingsRestController : protects endpoints
```

The diagram is intentionally a small domain view rather than a complete
dependency graph. The request DTOs are API-boundary types; `Booking` is the
persistence entity and domain data type.

## Architectural decisions

### Keep HTTP/JSON and SQLite for now

The browser and backend are part of one application, requests are synchronous,
and the application currently uses a single SQLite database. REST with JSON
is a direct fit for these boundaries and is straightforward to inspect, test,
and version. API routes use `/api/v1`; existing unversioned paths remain
compatibility aliases.

### Protocol Buffers

Do not add Protocol Buffer models at this stage. Protobuf is useful when there
is a concrete requirement for compact binary payloads, generated clients
across multiple independently deployed services, or a gRPC contract. It would
otherwise introduce a schema, code-generation step, and a second representation
of the same booking data without solving a current problem.

Keep the HTTP API's request/response types explicit (for example
`BookingRequest`) and validate them at the API boundary. If future clients
need stronger shared contracts, first consider publishing an OpenAPI
specification and generating or validating HTTP clients from it.

### Kafka

Kafka would be overkill for the current deployment. There is no independent
consumer, event-streaming requirement, or workload requiring asynchronous
decoupling. Adding a broker would increase installation, operations, data
consistency, and failure-handling complexity—especially for a local desktop
application backed by SQLite.

Revisit a message broker only when there is a specific need such as durable
integration events consumed by multiple independent services, asynchronous
work that must survive process restarts, or sustained throughput that the
current synchronous design cannot handle. A broker is not a replacement for
the application's SQLite persistence.

## Current boundaries and growth

- **Authentication and permissions:** the current backend has one configured
  administrator account and an administrator authority. It is not yet a
  multi-user management system with persisted users or per-user permissions.
- **Resources:** rooms and vehicles are persisted as managed resources; the
  existing booking `room` column stores their stable resource IDs.
- **Database:** the application uses SQLite and does not currently have a
  separate database migration service.
- **Deployment:** the backend and compiled frontend are one application
  artifact. Desktop packaging, Docker, and direct Maven execution are
  alternative launch/deployment paths.
- **API compatibility:** breaking external API changes should use a new major
  version prefix instead of changing `/api/v1` behavior.
