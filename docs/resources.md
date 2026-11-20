# Room and vehicle resources

The current administrator can manage the shared room/vehicle catalog from
`/?page=resources`. Add a room or car/bike by choosing its type and entering a
name. The saved resource is immediately available in the corresponding
booking form and timeline.

## Data model

Resources are stored in the `booking_resources` SQLite table with a generated
stable ID, type (`rooms` or `cars`), display name, and active flag. The
existing `bookings.room` column stores the resource ID. No existing booking
rows need to be rewritten. `schema.sql` creates the new table if necessary and
seeds the IDs and labels previously supplied by the frontend, so existing
bookings continue to resolve to their original resources.

The API returns both the stable `room` ID and a display-only `roomName` on
booking records. This keeps assignment and availability comparisons stable
while showing readable names in reports.

## Permissions and validation

The resource API is under `/api/v1/resources`. Listing and creation require
the administrator role; creation also requires a CSRF token. The backend
validates the type/name and rejects duplicate names within a type. Booking
create/update validates that the selected ID belongs to an active resource.

Resources are not deleted through the UI, so this initial management feature
cannot invalidate historical booking assignments. If resource editing or
retirement is added, retirement should mark a resource inactive while leaving
its ID and bookings intact.
