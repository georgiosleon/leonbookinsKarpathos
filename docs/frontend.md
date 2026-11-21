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
  active/paid and cancelled panels. Cancelled items are stacked so overlapping
  records, including cancellations for the same guest, remain visible as
  separate timeline items.
- Errors from booking, reports, and resource workflows appear inline and in a
  dismissible accessible popup. Dismissing the popup leaves the inline message
  visible. The booking screen can collapse the full booking form without
  clearing the current form values.
- A Help dialog is available from the booking, reports, and resource screens,
  with Installation and Operations sections. Installation summarizes portable
  app setup and the architecture/design documented in `docs/architecture.md`
  and `docs/backend.md`, and explains opt-in LAN access and its security
  limits. Operations covers booking validation and encrypted database backup
  and restore steps, including keeping the passphrase separate from the backup
  when sharing it through an approved support channel. The app does not email
  or upload database backups.
- The booking form supports create and update flows. The build-time
  `VITE_BOOKING_SAVE_MODE` setting selects the default (`create` or `edit`);
  the in-session control is intended for testing and lasts only for the
  current browser tab session.
- Reports can be opened with `?page=reports`; the UI filters bookings and can
  request the detailed HTML report or download a PDF of the selected resource
  type and date range. The PDF includes the page's summary, resource
  breakdown, and booking columns, with a landscape layout and repeated table
  headers across pages.
- English, Greek, German, Dutch, Danish, and French are supported. English is
  the default and fallback. Language and light/dark theme preferences are
  stored in the browser.
- The admin resource screen adds rooms and vehicles to the shared catalog.
  Active resources are selectable for new bookings; the booking stores the
  resource ID while API/report display uses its name.

## API integration

`src/bookingApi.js` centralizes calls to the same-origin backend. It uses
`/api/v1` endpoints, includes credentials through the browser session, fetches
the CSRF token for writes, and sends JSON for create, update, and delete
requests. API errors are surfaced to the UI rather than treating failed
requests as successful.

Translation catalogs live in `src/locales/<language>/translation.json`.
`npm run build` checks catalog keys and interpolation placeholders before
building the app; missing translations fall back to English.

`src/appConfig.js` validates the booking save-mode configuration and handles
its session-scoped override. Keep HTTP request details in the API module and
keep presentation and booking behavior in their corresponding UI/domain
modules instead of duplicating fetch logic across components.

The booking screen loads the global room and vehicle overlap-prevention flags
from `GET /api/v1/settings/availability` and uses them for immediate
client-side feedback. The backend remains authoritative; configure the flags
with `BOOKING_AVAILABILITY_ROOMS_PREVENT_OVERLAP` and
`BOOKING_AVAILABILITY_CARS_PREVENT_OVERLAP` in the application environment.
Both default to enabled. Cancelled bookings are excluded from conflict
checking.

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
