# Tests

## Running tests

Run the backend tests from the repository root with Java 25. Maven is
downloaded automatically by the checked-in wrapper on first use:

```sh
./mvnw test
```

In PowerShell, run `.\mvnw.cmd test`.

Run the frontend unit tests independently with Node.js 22 or newer:

```sh
cd frontend
npm ci
npm test
```

The desktop installer scripts run `./mvnw -B package` (or `mvnw.cmd` on
Windows), which includes the Maven test phase, before creating platform
packages. The frontend unit-test command is separate from Maven's frontend
production build.

## Current test suites

| Test file | Current focus |
|---|---|
| `src/test/java/com/leonsoft/api/BookingRequestTest.java` | Booking request validation and conversion rules. |
| `src/test/java/com/leonsoft/api/BookingsRestControllerTest.java` | REST booking controller behavior and request/response handling. |
| `src/test/java/com/leonsoft/api/BookingSecurityTest.java` | Authentication, authorization, and CSRF behavior for protected endpoints. |
| `src/test/java/com/leonsoft/services/BookingsServiceTest.java` | Booking service rules, derived values, and report behavior. |
| `src/test/java/com/leonsoft/services/BookingResourceServiceTest.java` | Resource creation, duplicate-name rejection, and active-resource validation. |
| `src/test/java/com/leonsoft/cli/DatabaseCliTest.java` | SQLite backup/restore validation and safety behavior. |
| `frontend/tests/bookingLogic.test.js` | Frontend booking/date logic, timeline filtering, report behavior, and theme preferences. |

These are focused automated tests; they are not a full end-to-end browser,
accessibility, desktop-platform, or load-test suite. Installer packages should
also be smoke-tested on their target operating systems, and UI changes should
be checked in a browser at relevant screen sizes.

## Extending coverage

- Add validation cases alongside the request DTO tests.
- Add booking rule and report cases in the service tests.
- Add endpoint and security cases in the controller/security tests.
- Add frontend domain behavior in the frontend unit tests.
- For changes crossing browser, HTTP, security, and database boundaries,
  consider a Spring integration test or an end-to-end browser test rather than
  relying on unit tests alone.
