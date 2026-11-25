# Room and vehicle resources

Administrators can manage the shared room/vehicle catalog from
`/?page=resources`. Add or edit a room or car by choosing its type (`rooms` or
`cars`) and entering a name; "Cars & bikes" in the UI is a label for the `cars`
type, not a separate type. Saved resources are immediately available in the
corresponding booking form and timeline.

## Data model

Resources are stored in the `booking_resources` SQLite table with a generated
stable ID, type (`rooms` or `cars`), display name, and active flag. Bookings
store the stable resource ID in `bookings.resource_id` (`resourceId`).

The API returns both `resourceId` and a display-only, `@Transient` `resourceName`
on booking records. When no name is populated, `BookingResponse` falls back to
the raw `resourceId` for `resourceName`, so reports may show the ID instead of a
readable name.

## Permissions and validation

The resource API is under `/api/v1/resources`. Listing is available to all
authenticated roles; creation requires the `ADMIN` role and a CSRF token. The
backend validates the type/name and rejects duplicate names within a type.
Booking create/update validates that the selected ID belongs to an active
resource.

Resources are not deleted through the UI, so management cannot invalidate
historical booking assignments. Existing resources can be edited via
`PUT /api/v1/resources/{id}`, including the active flag and compliance fields;
marking a resource inactive leaves its ID and bookings intact.
