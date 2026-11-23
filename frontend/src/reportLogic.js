import { isBookableStatus } from './bookingConfig.js';
import { europeanToIsoDate, isoToEuropeanDate } from './bookingLogic.js';

export function selectBookingsForReport(bookings, mode, startDate, endDate) {
  const start = normalizeIsoDate(startDate);
  const end = normalizeIsoDate(endDate);
  if (!start || !end || start > end) return [];

  const resourceIds = new Set(mode.resources.map(({ id }) => id));
  return bookings
    .filter((booking) => {
      const arrival = europeanToIsoDate(booking.startDate);
      return arrival && resourceIds.has(booking.resourceId) && arrival >= start && arrival <= end;
    })
    .sort((left, right) => (
      europeanToIsoDate(left.startDate).localeCompare(europeanToIsoDate(right.startDate)) ||
      String(left.name ?? '').localeCompare(String(right.name ?? ''))
    ));
}

export function summarizeBookings(bookings, mode) {
  const bookableBookings = bookings.filter((booking) => isBookableStatus(booking.status));
  const cancelledBookings = bookings.filter((booking) => booking.status === 'cancelled');

  const resources = mode.resources.map(({ id, text }) => {
    const resourceBookings = bookableBookings.filter((booking) => booking.resourceId === id);
    return {
      id,
      name: text,
      bookings: resourceBookings.length,
      guests: resourceBookings.reduce((total, booking) => total + (Number(booking.numOfGuests) || 0), 0),
      nights: resourceBookings.reduce((total, booking) => total + (Number(booking.numOfNights) || 0), 0),
      charge: sum(resourceBookings, 'charge'),
      received: sum(resourceBookings, 'received'),
      balance: sum(resourceBookings, 'balance'),
    };
  });

  return {
    totalBookings: bookings.length,
    activeBookings: bookableBookings.length,
    cancelledBookings: cancelledBookings.length,
    guests: bookableBookings.reduce((total, booking) => total + (Number(booking.numOfGuests) || 0), 0),
    nights: bookableBookings.reduce((total, booking) => total + (Number(booking.numOfNights) || 0), 0),
    charge: sum(bookableBookings, 'charge'),
    received: sum(bookableBookings, 'received'),
    balance: sum(bookableBookings, 'balance'),
    resources,
  };
}

function sum(bookings, field) {
  return bookings.reduce((total, booking) => total + (Number(booking[field]) || 0), 0);
}

function normalizeIsoDate(value) {
  const europeanDate = isoToEuropeanDate(value);
  return europeanToIsoDate(europeanDate) === value ? value : '';
}
