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
  with Installation, Operations, and Support sections. Installation summarizes
  portable app setup, licensing/activation, architecture/design, and opt-in LAN
  access. Operations covers booking validation and encrypted database backup and
  restore steps. Support shows diagnostics. The app does not email or upload
  database backups.
- The booking form supports create and update flows. The build-time
  `VITE_BOOKING_SAVE_MODE` setting selects the default (`create` or `edit`);
  the in-session control is intended for testing and lasts only for the
  current browser tab session.
- Reports can be opened with `?page=reports`; the UI filters bookings by arrival
  date and resource type and can request the detailed HTML report or download a
  PDF. The server PDF filters by resource type only; the selected dates appear
  in its title and do not filter its rows. It has its own summary, resource
  breakdown, and booking columns, with a landscape layout and repeated table
  headers across pages.
- English, Greek, German, Dutch, Danish, and French are supported. English is
  the default and fallback. Language and light/dark theme preferences are
  stored in the browser.
- The admin resource screen (`?page=resources`) adds and edits rooms and vehicles in the shared catalog, including the active flag and compliance fields. Active resources are selectable for new bookings; the booking stores the resource ID while API/report display uses its name when available.
- The admin user-management screen (`?page=users`) lists/creates/updates/deletes database-backed users. The UI gates actions by role, but backend authorization is authoritative.
- License activation is enforced before normal app use. The SPA fetches `/api/v1/license/status`; when unactivated, it shows a license gate. ADMIN can view the installation ID, manage the single customer record, and paste a signed license key to activate immediately. OPERATOR/GUEST see a blocked message.

## API integration

`src/bookingApi.js` centralizes calls to the same-origin backend. It uses
`/api/v1` endpoints, includes credentials through the browser session, fetches
the CSRF token for writes, and sends JSON for create, update, and delete
requests. `fetchCurrentUser()`/`/api/v1/users/me` provides role for UI gating.
API errors are surfaced to the UI; 401 redirects to login, 403 shows a permission error.

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
add a resource as an administrator.

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

Set seed/admin environment variables as needed before starting Spring Boot
(`APP_ADMIN_*`, `APP_OPERATOR_*`, `APP_GUEST_*`). The Vite
app communicates with the local backend; consult the Vite configuration and
package scripts in `frontend/` for the current development proxy and commands.
