# Tests

## Running tests

Run the backend tests from the repository root with Java 25. Maven is
downloaded automatically by the checked-in wrapper on first use:

```sh
./mvnw test
```

In PowerShell, run `.\mvnw.cmd test`.

Run frontend unit tests (Node-based domain/logic tests):

```sh
cd frontend
npm ci
npm run test:unit
```

Run frontend component tests (Vitest + React Testing Library + jsdom):

```sh
cd frontend
npm run test:components
```

`npm test` runs both frontend unit and component tests.

The desktop installer scripts run `./mvnw -B package` (or `mvnw.cmd` on
Windows), which includes the Maven test phase, before creating platform
packages. Frontend unit/component tests are separate from Maven's frontend
production build unless explicitly wired into CI.

## Current test suites

| Test file | Current focus |
|---|---|
| `src/test/java/com/leonsoft/api/BookingRequestTest.java` | Booking request validation and conversion rules. |
| `src/test/java/com/leonsoft/api/BookingsRestControllerTest.java` | REST booking controller behavior and request/response handling. |
| `src/test/java/com/leonsoft/api/BookingSecurityTest.java` | Authentication, authorization, CSRF, role matrix, license gate, and endpoint access behavior. |
| `src/test/java/com/leonsoft/config/DatabaseSchemaTest.java` | JPA startup/schema initialization smoke test on an empty SQLite database. |
| `src/test/java/com/leonsoft/config/DatabaseInitializerTest.java` | Idempotent default account seeding and configurable seed credentials. |
| `src/test/java/com/leonsoft/config/UserManagementTest.java` | User/role normalization and user-management related behavior. |
| `src/test/java/com/leonsoft/services/BookingsServiceTest.java` | Booking service rules, role-aware visibility/mutations, overlap policies, derived values, and report behavior. |
| `src/test/java/com/leonsoft/services/BookingsReportPdfServiceTest.java` | PDF export filtering and report PDF service behavior. |
| `src/test/java/com/leonsoft/services/BookingsPDFExporterTest.java` | PDF summary/resource sections, Unicode text, multi-page output, and repeated headers. |
| `src/test/java/com/leonsoft/services/BookingResourceServiceTest.java` | Resource creation, duplicate-name rejection, and active-resource validation. |
| `src/test/java/com/leonsoft/cli/DatabaseCliTest.java` | SQLite backup/restore validation, encrypted backup round-trip, wrong-passphrase handling, and overwrite safety. |
| `frontend/tests/bookingLogic.test.js` | Frontend booking/date logic, timeline filtering, report behavior, and theme preferences. |
| `frontend/tests/localeConfig.test.js` | Locale resolution and configuration behavior. |
| `frontend/src/components/admin/UserManagement.test.jsx` | User management UI load/create flow and access-denied error display. |
| `frontend/src/components/ReportsPage.test.jsx` | Reports page access-denied UX for unauthorized role. |
| `frontend/src/components/ResourcesPage.test.jsx` | Resources page access-denied UX for non-admin role. |
| `src/test/java/com/leonsoft/services/licensing/LicenseVerifierTest.java` | Offline Ed25519 license signature verification, install-ID binding, and tamper rejection. |

These are focused automated tests; they are not a full end-to-end browser,
accessibility, desktop-platform, or load-test suite. Installer packages should
also be smoke-tested on their target operating systems, and UI changes should
be checked in a browser at relevant screen sizes.

## Extending coverage

- Add validation cases alongside request DTO tests.
- Add booking/user role and report cases in service tests.
- Add endpoint and security cases in controller/security tests.
- Add frontend domain behavior in Node unit tests and UI/access-denied behavior in Vitest component tests.
- For changes crossing browser, HTTP, security, and database boundaries,
  consider a Spring integration test or an end-to-end browser test rather than
  relying on unit tests alone.


## Frontend E2E smoke (Playwright)

Playwright is scaffolded for optional browser smoke tests. Install browsers once:

```sh
cd frontend
npx playwright install
npm run test:e2e
```

Set `PLAYWRIGHT_BASE_URL` if the app is not running at `http://127.0.0.1:8080`.
