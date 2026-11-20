import test from 'node:test';
import assert from 'node:assert/strict';
import {
  defaultBookingSaveMode,
  getInitialBookingSaveMode,
  resolveBookingSaveMode,
  storeBookingSaveMode,
} from '../src/appConfig.js';
import { bookingModes, bookingsForTimeline } from '../src/bookingConfig.js';
import {
  bookingToForm,
  calculateBalance,
  europeanToIsoDate,
  hasAvailabilityConflict,
  isoToEuropeanDate,
  nightsBetween,
  toBookingPayload,
  toTimelineItem,
} from '../src/bookingLogic.js';
import {
  selectBookingsForReport,
  summarizeBookings,
} from '../src/reportLogic.js';

test('converts and validates the API date format', () => {
  assert.equal(isoToEuropeanDate('2026-03-08'), '08/03/2026');
  assert.equal(europeanToIsoDate('08/03/2026'), '2026-03-08');
  assert.equal(europeanToIsoDate('31/02/2026'), '');
  assert.equal(europeanToIsoDate('8/3/2026'), '');
});

test('calculates nights and balance', () => {
  assert.equal(nightsBetween('2026-03-08', '2026-03-11'), 3);
  assert.equal(nightsBetween('2026-03-11', '2026-03-08'), -3);
  assert.equal(calculateBalance('100.50', '20', '5.25'), 75.25);
});

test('availability conflicts only for overlapping bookable bookings in the same resource', () => {
  const existing = [{
    room: 'APT2',
    startDate: '10/07/2026',
    endDate: '13/07/2026',
    status: 'active',
  }];
  const candidate = {
    room: 'APT2',
    startDate: '12/07/2026',
    endDate: '14/07/2026',
    status: 'paid',
  };

  assert.equal(hasAvailabilityConflict(candidate, existing), true);
  assert.equal(hasAvailabilityConflict({ ...candidate, startDate: '13/07/2026' }, existing), false);
  assert.equal(hasAvailabilityConflict({ ...candidate, room: 'APT5' }, existing), false);
  assert.equal(hasAvailabilityConflict({ ...candidate, status: 'cancelled' }, existing), false);
  assert.equal(hasAvailabilityConflict(candidate, [{ ...existing[0], id: 'existing' }], 'existing'), false);
});

test('maps API records to form fields and back to the existing create payload shape', () => {
  const booking = {
    id: 'existing-id',
    name: 'Alex',
    numOfGuests: 2,
    agency: 'walkin',
    room: 'APT2',
    startDate: '08/03/2026',
    endDate: '11/03/2026',
    numOfNights: 3,
    charge: 100,
    received: 20,
    commission: 5,
    balance: 75,
    status: 'active',
  };
  const form = bookingToForm(booking);
  assert.equal(form.startDate, '2026-03-08');
  assert.equal(form.numOfGuests, '2');

  const payload = toBookingPayload(form);
  assert.equal(payload.startDate, '08/03/2026');
  assert.equal(payload.endDate, '11/03/2026');
  assert.equal(payload.numOfNights, 3);
  assert.equal(payload.balance, 75);
  assert.equal(Object.hasOwn(payload, 'id'), false);
});

test('creates safe timeline labels and the expected resource sets', () => {
  const item = toTimelineItem({
    id: 'booking-1',
    room: 'APT2',
    name: '<img src=x>',
    startDate: '08/03/2026',
    endDate: '11/03/2026',
    numOfNights: 3,
    numOfGuests: 2,
    status: 'active',
  }, bookingModes.rooms);

  assert.match(item.content, /&lt;img src=x&gt;/);
  assert.equal(bookingModes.rooms.resources.length, 11);
  assert.equal(bookingModes.cars.resources.length, 14);
});

test('separates cancelled records from active and paid timeline bookings', () => {
  const bookings = [
    { id: 'active', status: 'active' },
    { id: 'paid', status: 'paid' },
    { id: 'cancelled', status: 'cancelled' },
  ];

  assert.deepEqual(
    bookingsForTimeline(bookings, 'bookable').map(({ id }) => id),
    ['active', 'paid'],
  );
  assert.deepEqual(
    bookingsForTimeline(bookings, 'cancelled').map(({ id }) => id),
    ['cancelled'],
  );
  assert.throws(() => bookingsForTimeline(bookings, 'unknown'), /Unknown timeline type/);
});

test('marks cancelled timeline items for their separate visual treatment', () => {
  const item = toTimelineItem({
    id: 'cancelled-1',
    room: 'APT2',
    name: 'Alex',
    startDate: '08/03/2026',
    endDate: '11/03/2026',
    status: 'cancelled',
    agency: 'walkin',
  }, bookingModes.rooms);

  assert.equal(item.className, 'walkin booking-cancelled');
});

test('uses create-only by default and validates the global frontend save mode', () => {
  assert.equal(defaultBookingSaveMode, 'create');
  assert.equal(resolveBookingSaveMode(undefined), 'create');
  assert.equal(resolveBookingSaveMode('create'), 'create');
  assert.equal(resolveBookingSaveMode('edit'), 'edit');
  assert.throws(() => resolveBookingSaveMode('replace'), /Use "create" or "edit"/);
});

test('stores the UI save-mode choice for the current browser session', () => {
  const previousWindow = globalThis.window;
  const values = new Map();
  globalThis.window = {
    sessionStorage: {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
    },
  };

  try {
    assert.equal(getInitialBookingSaveMode(), 'create');
    assert.equal(storeBookingSaveMode('edit'), 'edit');
    assert.equal(getInitialBookingSaveMode(), 'edit');
    assert.equal(storeBookingSaveMode('create'), 'create');
    assert.equal(getInitialBookingSaveMode(), 'create');
  } finally {
    if (previousWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = previousWindow;
    }
  }
});

test('filters report records by selected mode and inclusive arrival-date range', () => {
  const bookings = [
    { id: 'inside', room: 'APT2', startDate: '10/07/2026', status: 'active' },
    { id: 'before', room: 'APT2', startDate: '30/06/2026', status: 'active' },
    { id: 'other-mode', room: 'CAR1', startDate: '10/07/2026', status: 'active' },
    { id: 'cancelled', room: 'APT2', startDate: '10/07/2026', status: 'cancelled' },
  ];

  assert.deepEqual(
    selectBookingsForReport(bookings, bookingModes.rooms, '2026-07-01', '2026-07-31')
      .map(({ id }) => id),
    ['inside', 'cancelled'],
  );
  assert.deepEqual(
    selectBookingsForReport(bookings, bookingModes.rooms, '2026-07-31', '2026-07-01'),
    [],
  );
});

test('summarizes active financials by resource and counts cancellations separately', () => {
  const summary = summarizeBookings([
    {
      id: 'active',
      room: 'APT2',
      status: 'active',
      numOfGuests: 2,
      numOfNights: 3,
      charge: 300,
      received: 100,
      balance: 150,
    },
    {
      id: 'paid',
      room: 'APT2',
      status: 'paid',
      numOfGuests: 1,
      numOfNights: 1,
      charge: 100,
      received: 100,
      balance: 0,
    },
    {
      id: 'cancelled',
      room: 'APT2',
      status: 'cancelled',
      numOfGuests: 4,
      numOfNights: 5,
      charge: 500,
      received: 250,
      balance: 250,
    },
  ], bookingModes.rooms);

  assert.equal(summary.totalBookings, 3);
  assert.equal(summary.activeBookings, 2);
  assert.equal(summary.cancelledBookings, 1);
  assert.equal(summary.guests, 3);
  assert.equal(summary.nights, 4);
  assert.equal(summary.charge, 400);
  assert.equal(summary.received, 200);
  assert.equal(summary.balance, 150);
  assert.deepEqual(summary.resources[0], {
    id: 'APT2',
    name: 'APT 2',
    bookings: 2,
    guests: 3,
    nights: 4,
    charge: 400,
    received: 200,
    balance: 150,
  });
});
