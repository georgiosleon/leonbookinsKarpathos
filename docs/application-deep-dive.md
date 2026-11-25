# Application deep dive

This page expands the short overview in the top-level
[README](../README.md#application-overview). Treat the source code and tests as
authoritative when behavior changes. Related docs:
[architecture](architecture.md), [backend](backend.md), [frontend](frontend.md),
[resources](resources.md), and [testing](testing.md).

## Product and domain

Leon Bookings (Karpathos) is a local/self-hosted **room and vehicle booking
planner**. Authenticated users manage reservations for apartments/studios and
cars/bikes, with timeline visualization, financial fields (charge / received /
commission / balance), localized reports (HTML + PDF), and a managed resource
catalog.

It is a **single-process** application, not a multi-tenant SaaS. Authentication
uses database-backed users with `ADMIN`, `OPERATOR`, and `GUEST` roles; default seed
accounts can be configured via environment variables and are created only when
missing. Booking data lives in SQLite on the host that runs the process.

Primary entry points for humans: top-level `README.md`, this deep dive, and the
rest of `docs/`.

## Tech stack

| Layer | Technology |
|--------|------------|
| Backend | Java **25**, Spring Boot **4.1**, Spring MVC, Spring Security, Bean Validation, Spring Data JPA |
| Database | **SQLite** (`sqlite-jdbc` + Hibernate community SQLite dialect) |
| Reports | OpenPDF, HTML template + i18n property bundles |
| Frontend | **React 19** + **Vite**, i18next / react-i18next; **vis-timeline** as a static browser script |
| Build / packaging | Maven (`pom.xml` + wrapper), `frontend-maven-plugin` (pinned Node/npm), Spring Boot fat JAR |
| Deploy | Docker (`Dockerfile` + `compose.yaml`), desktop via `jpackage` under `install/` |

Application entry: `com.leonsoft.Application`
(`src/main/java/com/leonsoft/Application.java`).

## High-level architecture

One deployable artifact: the **React SPA** is built into static assets and
served by Spring Boot. Browser and API share the same origin.

```text
Browser → React UI → /api/v1/* → Controllers → Services → JPA Repositories → SQLite
                                         ↘ Report HTML/PDF
Desktop launcher (optional) → starts JAR + opens browser
Offline CLI: java -jar … db backup|restore  (before Spring starts)
```

Profiles: `dev` (default for direct runs) vs `prod` (Docker Compose forces
`prod`). See `application.properties`, `application-dev.properties`, and
`application-prod.properties`. Diagrams and design decisions:
[architecture.md](architecture.md).

## Domain entities and features

### Booking

Entity: `models/Booking.java` (table `bookings`).

- Stay: `startDate`, `endDate`, `numOfNights`
- Money: `charge`, `received`, `commission`, `balance` (balance calculated
  server-side)
- Guest / ops: `name`, `agency`, `numOfGuests`, `country`, `email`, `tel`,
  `voucher`, `identification`, `invoiceNumber`, `taxRefNumber`, `afm`,
  `extraInfo`
- Resource: `resourceId` stores the **stable resource ID**; `resourceName` is a
display-only field resolved for responses/reports
- `status`: `active` | `paid` | `cancelled` (validated on the request DTO)
- `requestDateTime`

### BookingResource

Entity: `models/BookingResource.java` (table `booking_resources`).

- `id`, `type` (`rooms` | `cars`), `name`, `active`
- Resource catalog is managed by admin through the API/UI; existing bookings keep
  stored resource assignments.

### Users

Entity: `models/User.java` (table `users`). Users have `username`,
`passwordHash`, `email`, `role` (`ADMIN`/`OPERATOR`/`GUEST`), and `active` flags.
`DatabaseInitializer` seeds missing default accounts on startup using
`APP_ADMIN_*`, `APP_OPERATOR_*`, and `APP_GUEST_*` environment variables; existing
accounts are not overwritten. Passwords are BCrypt-hashed.

### Features

- Shared booking UI for rooms vs vehicles (`?type=rooms` / `?type=cars`)
- Dual timelines: active/paid vs cancelled (vis-timeline)
- Overlap prevention for active/paid bookings (env:
  `BOOKING_AVAILABILITY_*_PREVENT_OVERLAP`; exposed via settings API)
- Create / update / delete bookings with validated DTOs
- Reports: browser/CSV by arrival date, server HTML by full stay containment,
  PDF by resource type only; detailed text report + PDF
- Admin resource catalog (`/?page=resources`) — add and edit resources, including
  active flag and compliance fields
- i18n: en, el, de, nl, da, fr
- Light/dark theme (browser preference)
- Encrypted/unencrypted SQLite backup/restore CLI
- Offline one-time license activation per installation (ADMIN, signed Ed25519 key, DB install ID)
- Help / diagnostics / app-info support endpoints

## Key packages and directories

| Path | Role |
|------|------|
| `src/main/java/com/leonsoft/api/` | REST controllers + request DTOs |
| `src/main/java/com/leonsoft/services/` | Booking rules, availability, resources, PDF/HTML reports |
| `src/main/java/com/leonsoft/repositories/` | `BookingRepository`, `BookingResourceRepository`, `UserRepository` |
| `src/main/java/com/leonsoft/models/` | JPA entities (`Booking`, `BookingResource`, `User`, `Role`) |
| `src/main/java/com/leonsoft/config/` | Security, DB/SQLite, MVC/SPA fallback, validation, seed initialization |
| `src/main/java/com/leonsoft/cli/` | Offline `DatabaseCli` / encrypted backup |
| `src/main/resources/public/` | SPA shell, redirects, shared CSS/JS (vis-timeline, etc.) |
| `src/main/resources/public/react-assets/` | Vite build output (Maven-generated) |
| `frontend/` | React source, locales, Vite config, unit tests |
| `docs/` | Architecture, frontend, backend, testing, resources |
| `install/` | Desktop launcher + packaging scripts |
| `compose.yaml`, `Dockerfile` | Container run |
| `src/test/` | Backend tests |

## API surface

Versioned under **`/api/v1`**, the only supported prefix; unversioned `/booking/...`
and `/csrf` aliases were removed in 2.0.0. Breaking changes should use a new
major prefix (for example `/api/v2`).

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/booking/getAll` | List bookings |
| POST | `/api/v1/booking/save` | Create |
| PUT | `/api/v1/booking/update` | Update |
| POST | `/api/v1/booking/delete` | Delete by JSON `{ id }` |
| POST | `/api/v1/booking/del?bid=` | Legacy: delete **cancelled** only |
| GET | `/api/v1/booking/report` | Localized detailed report (text) |
| GET | `/api/v1/booking/export/pdf` | PDF (`fromDate`, `toDate`, `type`, `lang`) |
| GET | `/api/v1/resources` | List rooms/cars |
| POST | `/api/v1/resources` | Create resource |
| GET | `/api/v1/settings/availability` | Overlap flags |
| GET | `/api/v1/license/status` | License status/install ID |
| POST | `/api/v1/license/activate` | Activate license (ADMIN) |
| GET/POST/PUT/DELETE | `/api/v1/customers...` | Licensed customer CRUD (ADMIN) |
| GET | `/api/v1/csrf` | CSRF token for writes |
| GET | `/api/v1/app-info` | App metadata |
| GET | `/api/v1/diagnostics` | Diagnostics download |
| GET | `/login` | Form login page |
| POST | `/logout` | Logout |

Controllers include `BookingsRestController`, `BookingResourceController`,
`AvailabilitySettingsRestController`, `CsrfController`,
`ApplicationSupportController`, `LoginPageController`, and
`ErrorRestController`.

Security: database-backed users with `ADMIN`/`OPERATOR`/`GUEST` roles.
Admins manage users, resources, and admin ops and can view reports; operators manage bookings and view reports;
guests have read-only booking/resource visibility. CSRF cookie protects mutating browser calls. Frontend client modules:
`frontend/src/bookingApi.js`, `frontend/src/usersApi.js`.

## Frontend structure and flows

Routing is query-param based in `frontend/src/main.jsx`:

- Default → `BookingPage` (`/?type=rooms|cars`)
- `?page=reports` → `ReportsPage`
- `?page=resources` → `ResourcesPage`
- `?page=users` → `UserManagement` (admin)

Main modules:

- `components/BookingPage.jsx` — load bookings/resources/settings, form +
  timelines, create/edit
- `components/BookingForm.jsx`, `BookingTimeline.jsx`
- `components/ReportsPage.jsx` — filters, HTML report, PDF export
- `components/ResourcesPage.jsx` — add/edit rooms and cars
- `bookingLogic.js`, `bookingConfig.js`, `appConfig.js` (save mode create/edit)
- `locales/*/translation.json` — i18n catalogs

Build output goes to `src/main/resources/public/react-assets/`
(`frontend/vite.config.js`). The SPA shell
`src/main/resources/public/index.html` mounts `#root` and loads
`/react-assets/main.js`. Legacy redirects: `rooms.html` → `/?type=rooms`,
`cars.html` → `/?type=cars`.

Dev: `cd frontend && npm run dev` (Vite proxies selected paths to `:8080`).
Maven still builds the SPA for packaged runs.

## How to run and deploy

1. **Docker** — `cp .env.example .env`, set seed account credentials,
   `docker compose up --build -d` → `http://localhost:8080`. Data lives in
   volume `booking-data`.
2. **Maven** — set `APP_ADMIN_*` / `APP_OPERATOR_*` / `APP_GUEST_*` as needed, then
   `./mvnw generate-resources spring-boot:run` (or `package`). Default DB is
   `bookings.db`; override with `APP_DB_URL`.
3. **Desktop** — `install/package.sh` / `package.ps1` → portable archive / MSI /
   DMG with bundled JRE. See [`install/README.md`](../install/README.md).
4. **CLI** — `java -jar target/leonbookinsKarpathos-2.0.2.jar db backup|restore …`
   (dispatched in `Application.main` before Spring).

Default bind is often loopback; LAN access needs `SERVER_ADDRESS=0.0.0.0` or
Compose `BOOKING_HOST_ADDRESS`. The app does not terminate TLS itself.

## Current boundaries and legacy notes

React is the **current** UI (Vite build is part of the Maven lifecycle), not an
unfinished swap. What remains transitional or deferred:

- Old static HTML backups under `src/main/resources/static/bckup/` and leftover
  libs (for example `public/js/w2ui.js`) are not the active app path
- `/api/v1` is the only supported API prefix; unversioned aliases were removed in
  2.0.0 (`/api/v1/booking/del` remains as a legacy delete route)
- Schema evolution is handled by Flyway SQL migrations with Hibernate `ddl-auto=validate`.
- User management exists (ADMIN CRUD), but user self-service/profile/password
  flows and finer-grained permissions are still limited
- Resources can be added and edited, including the active flag and compliance
  fields (see [resources.md](resources.md))

## Mental model

One Spring Boot JAR embeds a React booking SPA, talks to itself over `/api/v1`,
stores bookings, users, and a room/car catalog in SQLite, enforces database-backed
role authorization (`ADMIN`/`OPERATOR`/`GUEST`) with session + CSRF,
enforces offline one-time license activation per installation, and can be launched as Docker, Maven, or a desktop wrapper with an offline backup CLI.
