import test from 'node:test';
import assert from 'node:assert/strict';
import { parseBookingEmail } from '../src/bookingEmailImport.js';

// Sanitized fixtures: labels, dates, and structure come from real templates,
// while names, numbers, and addresses are deliberately fake.
const enConfirmation = `Subject: Booking.com: Confirmation for Test Hotel (12 Dec 2026 - 15 Dec 2026) | PIN: 0000

Booking confirmed!
Your reservation at Test Hotel is confirmed.

Reservation details:
Booking number: 1000000001
PIN code: 0000
Guest name: Test Guest
Property: Test Hotel
Address: Test Street 1, 12345 Test Town, Greece
Phone: +30 00000 00000

Check-in: Saturday, 12 December 2026 (From 14:00)
Check-out: Tuesday, 15 December 2026 (Until 11:00)

Rooms & Guests:
1 x Test Studio — 2 adults

Price breakdown:
Room price: €240.00
Taxes and charges (15%): €36.00
Total price: €276.00

Payment information:
The property will charge €276.00 on 05 December 2026.
`;

const enUpdate = `Subject: Booking.com: Updated confirmation for Test Hotel Two (04 Jan 2027 - 07 Jan 2027)

Your reservation details have been updated.

Booking reference: 1000000002
Guest(s): Guest One, Guest Two
Accommodation: Test Hotel Two
Check-in: Monday, 04 January 2027 (15:00 - 22:00)
Check-out: Thursday, 07 January 2027 (08:00 - 11:00)

Booked unit:
Test Superior Room
Max guests: 2 adults, 1 child (age 6)

Total amount: €420.00 (Prepaid via Booking.com)
Status: Confirmed
`;

const elConfirmation = `Subject: Booking.com: Επιβεβαίωση για Test Hotel (12 Δεκ 2026 - 15 Δεκ 2026) | PIN: 0000

Η κράτησή σας επιβεβαιώθηκε!
Η κράτησή σας στο κατάλυμα Test Hotel επιβεβαιώθηκε.

Στοιχεία κράτησης:
Αριθμός κράτησης: 1000000001
Κωδικός PIN: 0000
Όνομα επισκέπτη: Δοκιμαστικός Επισκέπτης
Κατάλυμα: Test Hotel
Διεύθυνση: Οδός Δοκιμής 1, 12345 Δοκιμαστική Πόλη, Ελλάδα
Τηλέφωνο: +30 00000 00000

Check-in: Σάββατο, 12 Δεκεμβρίου 2026 (Από 14:00)
Check-out: Τρίτη, 15 Δεκεμβρίου 2026 (Έως 11:00)

Δωμάτια & Επισκέπτες:
1 x Test Studio — 2 ενήλικες

Ανάλυση τιμής:
Τιμή δωματίου: € 240,00
Φόροι και χρεώσεις (15%): € 36,00
Συνολική τιμή: € 276,00
`;

const elUpdate = `Subject: Booking.com: Ενημερωμένη επιβεβαίωση για Test Hotel Two (04 Ιαν 2027 - 07 Ιαν 2027)

Τα στοιχεία της κράτησής σας ενημερώθηκαν.

Αριθμός αναφοράς κράτησης: 1000000002
Επισκέπτης(-ες): Επισκέπτης Ένας, Επισκέπτης Δύο
Κατάλυμα: Test Hotel Two
Check-in: Δευτέρα, 04 Ιανουαρίου 2027 (15:00 - 22:00)
Check-out: Πέμπτη, 07 Ιανουαρίου 2027 (08:00 - 11:00)

Μονάδα που κρατήθηκε:
Test Superior Room
Μέγιστος αριθμός επισκεπτών: 2 ενήλικες, 1 παιδί (ηλικίας 6 ετών)

Συνολικό ποσό: € 420,00 (Προπληρωμένο μέσω Booking.com)
Κατάσταση: Επιβεβαιωμένη
`;

test('parses the English confirmation template', () => {
  const result = parseBookingEmail(enConfirmation);
  assert.equal(result.detectedLanguage, 'en');
  assert.deepEqual(result.values, {
    platformReservationId: '1000000001',
    name: 'Test Guest',
    startDate: '2026-12-12',
    endDate: '2026-12-15',
    numOfGuests: '2',
    agency: 'bookingcom',
  });
  assert.equal(result.propertyName, 'Test Hotel');
  assert.equal(result.notes.length, 1);
  assert.equal(result.notes[0].value, '€276.00');
  for (const warning of ['PROPERTY_PHONE_IGNORED', 'PROPERTY_ADDRESS_IGNORED', 'PIN_IGNORED', 'MONEY_NOT_FILLED']) {
    assert.ok(result.warnings.includes(warning), warning);
  }
  assert.ok(!result.warnings.includes('GUEST_COUNT_MISSING'));
});

test('parses the English update template and joins multiple guests', () => {
  const result = parseBookingEmail(enUpdate);
  assert.equal(result.detectedLanguage, 'en');
  assert.equal(result.values.platformReservationId, '1000000002');
  assert.equal(result.values.name, 'Guest One, Guest Two');
  assert.equal(result.values.startDate, '2027-01-04');
  assert.equal(result.values.endDate, '2027-01-07');
  assert.equal(result.values.numOfGuests, undefined);
  assert.equal(result.values.status, undefined);
  assert.equal(result.values.charge, undefined);
  assert.equal(result.multipleGuests, true);
  assert.equal(result.unitName, 'Test Superior Room');
  assert.ok(result.warnings.includes('GUEST_COUNT_MISSING'));
  assert.ok(result.warnings.includes('MULTIPLE_GUESTS'));
});

test('parses the Greek confirmation template with Greek month names', () => {
  const result = parseBookingEmail(elConfirmation);
  assert.equal(result.detectedLanguage, 'el');
  assert.equal(result.values.platformReservationId, '1000000001');
  assert.equal(result.values.name, 'Δοκιμαστικός Επισκέπτης');
  assert.equal(result.values.startDate, '2026-12-12');
  assert.equal(result.values.endDate, '2026-12-15');
  assert.equal(result.values.numOfGuests, '2');
  assert.equal(result.values.tel, undefined);
  assert.equal(result.values.country, undefined);
  assert.equal(result.notes[0].value, '€ 276,00');
  assert.ok(result.warnings.includes('PROPERTY_PHONE_IGNORED'));
});

test('parses the Greek update template and ignores capacity as guest count', () => {
  const result = parseBookingEmail(elUpdate);
  assert.equal(result.detectedLanguage, 'el');
  assert.equal(result.values.platformReservationId, '1000000002');
  assert.equal(result.values.name, 'Επισκέπτης Ένας, Επισκέπτης Δύο');
  assert.equal(result.values.startDate, '2027-01-04');
  assert.equal(result.values.endDate, '2027-01-07');
  assert.equal(result.values.numOfGuests, undefined);
  assert.equal(result.notes[0].value, '€ 420,00');
  assert.ok(result.warnings.includes('GUEST_COUNT_MISSING'));
});

test('honours a forced language override', () => {
  const result = parseBookingEmail(elConfirmation, 'en');
  assert.equal(result.detectedLanguage, 'en');
  assert.deepEqual(result.values, {});
  assert.ok(result.warnings.includes('NOTHING_FOUND'));
  assert.ok(result.warnings.includes('RESERVATION_ID_MISSING'));
  assert.ok(result.warnings.includes('DATES_MISSING'));
});

test('returns nothing useful for unrelated text', () => {
  const result = parseBookingEmail('Just a note about next week.');
  assert.deepEqual(result.values, {});
  assert.ok(result.warnings.includes('NOTHING_FOUND'));
});
