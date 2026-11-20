# Frontend

## Technology and build

The frontend is a React application built with Vite under `frontend/`. The
Maven build uses the frontend Maven plugin to install its pinned Node.js/npm
versions, run `npm ci`, and build the production assets. This lets a Maven
package include the frontend without requiring a separately installed Node.js
for the production build. For frontend development and its unit tests, use
Node.js 22 or newer and npm.

The production frontend is served by Spring Boot from the packaged static
resources. Vite development mode serves the app separately and expects the
backend at its configured local address.

## UI responsibilities

- A shared booking interface supports room and vehicle modes, selected by
  query parameters such as `/?type=rooms` and `/?type=cars`.
- The existing `vis-timeline` browser component displays bookings in separate
  active/paid and cancelled panels.
- The booking form supports create and update flows. The build-time
  `VITE_BOOKING_SAVE_MODE` setting selects the default (`create` or `edit`);
  the in-session control is intended for testing and lasts only for the
  current browser tab session.
- Reports can be opened with `?page=reports`; the UI filters bookings and
  requests HTML/text and PDF report endpoints.
- English and Greek are supported. Language and light/dark theme preferences
  are stored in the browser.
- The admin resource screen adds rooms and vehicles to the shared catalog.
  Active resources are selectable for new bookings; the booking stores the
  resource ID while API/report display uses its name.

## API integration

`src/bookingApi.js` centralizes calls to the same-origin backend. It uses
`/api/v1` endpoints, includes credentials through the browser session, fetches
the CSRF token for writes, and sends JSON for create, update, and delete
requests. API errors are surfaced to the UI rather than treating failed
requests as successful.

`src/appConfig.js` validates the booking save-mode configuration and handles
its session-scoped override. Keep HTTP request details in the API module and
keep presentation and booking behavior in their corresponding UI/domain
modules instead of duplicating fetch logic across components.

The resource catalog is loaded from the backend; the frontend does not contain
a second hardcoded copy of the room/car inventory. Visit `/?page=resources` to
add a resource as the current administrator.

## Development

From `frontend/`:

```sh
npm ci
npm run dev
```

Run the unit tests with:

```sh
npm test
```

Set the backend administrator environment variables before starting Spring
Boot. The Vite app communicates with the local backend; consult the Vite
configuration and package scripts in `frontend/` for the current development
proxy and available commands.
