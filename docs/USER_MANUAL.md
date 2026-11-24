# User manual — booking planner UI

This manual covers the **day-to-day screens a regular user works with**:

- signing in and out,
- the rooms/cars booking pages,
- timelines,
- booking reports and exports,
- language, theme, and other small UI controls.

It does **not** cover user management, resource/catalog management, licensing,
backup/restore, settings, or deployment. If you see a screen for those topics,
it is outside the scope of this manual.

---

## 1. Getting started

### Sign in

1. Open the address your administrator gave you.
2. On the sign-in page, enter your username and password.
3. Select **Sign in**.

After signing in, the app opens the **Rooms** planner by default. If your access
has expired, the app sends you back to the sign-in page. Sign in again to
continue; unsaved form changes are not kept.

### The header

The header stays the same on the main screens:

| Control | What it does |
|---|---|
| Current date and clock | Shows the local date/time used by the UI. |
| **Rooms** | Opens the room/studio planner. |
| **Cars & bikes** | Opens the vehicle planner. |
| **Reports** | Opens reports for the current mode. Shown only if your role allows it. |
| Theme toggle | Switches between light and dark appearance. |
| Language selector | Changes the UI language. Your choice is remembered in this browser. |
| **Sign out** | Ends your session. |

Management links may also appear in the navigation for some roles; they are not
covered here.

---

## 2. Rooms and cars are separate workspaces

The app has two booking modes:

- **Rooms** — apartments/studios. The resource field is called **Room**.
- **Cars & bikes** — vehicles. The resource field is called **Vehicle**.

Switching modes changes the resource list, the labels in the form and reports,
and the booking colors/channels. A booking made in one mode does not appear in
the other mode. The **Reports** link keeps the mode you are currently in.

---

## 3. The booking page

The booking page has three main areas:

1. **Booking details panel** — the form, plus a short live summary of name,
   dates, telephone, estimated subtotal, and estimated balance.
2. **Availability timeline** — active and paid bookings.
3. **Cancelled bookings timeline** — cancelled bookings, stacked so multiple
   cancellations for the same resource stay visible.

Use **Hide form** / **Show form** in the booking details panel to collapse or
expand the form without leaving the page.

The timeline toolbar has these buttons:

| Button | Effect |
|---|---|
| **Go to start date** | Moves both timelines to the start date currently in the form. |
| **Today** | Moves both timelines to today. |
| **90 days back** / **30 days back** | Scrolls the timelines backward. |
| **30 days forward** / **90 days forward** | Scrolls the timelines forward. |

You can also drag the timeline horizontally and use `Ctrl` + mouse wheel to
zoom in or out.

---

## 4. Create a booking

1. Open **Rooms** or **Cars & bikes**.
2. Fill in the form. Required fields are marked with `*`.
3. Check the live summary and timeline for conflicts.
4. Select **Save as new booking**.

### Stay / Reservation

| Field | What to enter |
|---|---|
| **From \*** | Arrival date. Changing it also resets the departure date to the same day. |
| **To \*** | Departure date. Must be after the arrival date. |
| **Nights** | Calculated automatically; you cannot edit it. |
| **Room/Vehicle \*** | Select the resource for this booking. Inactive resources are marked `(Inactive)`. |
| **Status \*** | `Active`, `Paid`, or `Cancelled`. Active and paid bookings appear in the availability timeline; cancelled bookings appear in the cancelled timeline. |
| **Guests \*** | Number of guests, from 1 to 6. |
| **Save mode** | Choose whether saving creates a new booking or updates the selected one. See below. |

### Guest & Channel

| Field | What to enter |
|---|---|
| **Name \*** | Guest name. It appears on the timeline and in reports. |
| **Agency / color \*** | Booking channel. For rooms: AirBnB, Booking.com, Walk-in, Closed. For vehicles: DEFAULT, RED, BLUE, GREEN, Unavailable. The choice colors the timeline item. |
| **Country** | Optional country. |

### Financials

| Field | What it means |
|---|---|
| **Total charged (€)** | Amount charged to the guest. |
| **Commission (€)** | Commission to subtract. |
| **Received (€)** | Amount already received. |
| **Rental tax per night (€)** | Read-only rate from the selected room/vehicle. |
| **Rental tax (€)** | Displayed estimate: nights × nightly tax rate. |
| **Rental subtotal (€)** | Displayed estimate: total charged − rental tax. |
| **Balance (€)** | Displayed estimate: total charged − received − commission − rental tax. |

These money fields are **estimates while you type**. The app recalculates and
stores the final tax, nights, subtotal, and balance on save using the selected
resource's current rate. If you change the resource or dates after loading a
booking, the saved values can differ from the earlier estimate.

Use **Hide financials** / **Show financials** to collapse or expand this section.

### Details

Select **Show details** to enter optional information:

- Passport / ID
- Tax number (ΑΦΜ)
- Email
- Telephone
- Voucher
- Tax reference
- Invoice number
- Platform reservation ID
- Notes

Email must be a valid address if you enter one.

### Save mode

The **Save mode** button switches between:

| Mode | Behavior |
|---|---|
| **Remove & add mode** | Saving creates a new booking. If a booking is selected, it is left unchanged. |
| **Update mode** | Saving updates the selected booking. If nothing is selected, saving still creates a new booking. |

Your choice is remembered for the current browser tab session.

### Validation and errors

Before saving, the app checks:

- required fields are filled,
- the departure date is after the arrival date,
- the email address is valid if provided,
- the resource is available for those dates when overlap prevention is enabled.

Problems are shown both inline near the form and in a popup dialog. Close the
popup with its **Close** button, fix the highlighted field, and try again.

After a successful save, a success message appears and the booking is added to
or updated in the timeline.

---

## 5. Work with the timelines

### Find and review a booking

1. Use the toolbar to move to the relevant dates.
2. Find the booking in the **active/paid** or **cancelled** timeline.
3. Select the item.

The form loads that booking, and the page summary updates. Timeline items show
the guest name, nights, guest count, and an icon depending on the mode. Their
color reflects the selected agency/channel.

### Change a booking

1. Select the booking on a timeline.
2. Change the fields you need in the form.
3. Switch to **Update mode**.
4. Select **Update booking**.

To cancel a reservation, load it, change **Status** to **Cancelled**, and save
in **Update mode**. It moves from the availability timeline to the cancelled
timeline.

### Copy a booking

1. Select the booking you want to reuse.
2. Keep **Remove & add mode**.
3. Change the dates or other details.
4. Select **Save as new booking**.

The original booking remains unchanged.

### Remove a booking

If your role allows booking deletion, the timeline supports removing an item.
A confirmation prompt asks **“Delete booking for <name>?”**. Confirm only when
you intend to remove that booking permanently. After deletion, a success
message appears and the item disappears from the timeline.

If you do not have permission, the deletion control is unavailable.

---

## 6. Reports

Open **Reports** from the header. Reports follow the current mode, so use
**Rooms** or **Cars & bikes** first if you need to switch. Reports are available
to users with report access; otherwise an access message is shown.

### Choose the period

- The page starts with the current calendar month.
- Use **From** and **To** to choose the dates.
- A booking is included when its **arrival date** falls inside the range.
- Cancelled bookings appear in the record list but are excluded from
  occupancy and financial totals.

### On-screen results

The page shows:

- summary cards for bookings, cancelled bookings, guests, nights, total
  charged, received, rental tax, rental subtotal, and balance;
- a per-resource summary table;
- a detailed booking table with dates, resource, guest, channel, platform
  reservation ID, guests, nights, tax, charges, received amount, subtotal, and
  balance.

### Detailed report

Select **Detailed report** to open a formatted HTML version in a dialog. Use
**Close** or click outside the dialog to return to the page.

### Exports

| Button | Result |
|---|---|
| **Export CSV** | Downloads a spreadsheet-ready file named for the mode and selected dates. The CSV headers are in English. |
| **Export PDF** | Downloads a PDF generated by the server. |

For totals and records that match the selected period exactly, use the on-screen
tables or **Export CSV**. The server-side PDF export currently uses its own
filtering, so it may not match the selected period in every case.

---

## 7. Common UI controls and messages

| Control/message | Meaning |
|---|---|
| **New / reset** | Clears the form and deselects the current booking. |
| **Show details** / **Hide details** | Expands or collapses the optional guest/channel fields. |
| **Hide financials** / **Show financials** | Expands or collapses money fields. A compact summary remains visible. |
| Inline error + popup | Something needs correction. Fix the field named in the message. |
| Success message | The save, update, or delete completed. |
| `Room/Vehicle is not available for those dates.` | The dates overlap an existing active/paid booking for that resource. |
| Disabled form fields | Your role allows viewing but not editing. Ask your administrator if you need edit access. |
| Sign-in page appears again | Your session ended. Sign in again; unsaved changes are lost. |

---

## 8. Quick troubleshooting

**I cannot see a booking.**

- Make sure you are in the right mode: **Rooms** or **Cars & bikes**.
- Move the timeline with **Today**, **Go to start date**, or the date-shift buttons.
- Check the **Cancelled bookings** timeline if the booking was cancelled.
- On **Reports**, remember that inclusion is by arrival date.

**The timeline looks empty.**

Use **Today** or **Go to start date**; the initial view centers on a window
around today. The count beside each timeline heading shows how many bookings
are currently visible.

**The balance changed after saving.**

The values shown in the form before saving are estimates. The app recalculates
them from the selected resource's current nightly tax rate when it saves.

**My form is read-only.**

Your role can view bookings but not change them. No data is lost; you can still
use timelines and reports if your role allows them.

**The PDF does not match the on-screen report.**

Use the on-screen tables or CSV export for the period you selected. The PDF is
generated separately by the server and may use different filtering.
