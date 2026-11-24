const DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

export function isoToEuropeanDate(value) {
  if (!value) return '';
  const [year, month, day] = value.split('-');
  if (!year || !month || !day) return '';
  return `${day}/${month}/${year}`;
}

export function europeanToIsoDate(value) {
  if (!value) return '';
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (!match) return '';

  const [, dayText, monthText, yearText] = match;
  const day = Number(dayText);
  const month = Number(monthText);
  const year = Number(yearText);
  const date = new Date(Date.UTC(year, month - 1, day));

  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return '';
  }

  return `${yearText}-${monthText}-${dayText}`;
}

export function nightsBetween(startDate, endDate) {
  if (!startDate || !endDate) return 0;
  const start = new Date(`${startDate}T00:00:00Z`);
  const end = new Date(`${endDate}T00:00:00Z`);
  const days = (end.getTime() - start.getTime()) / DAY_IN_MILLISECONDS;
  return Number.isInteger(days) ? days : 0;
}

// Display-only estimates. The backend is authoritative and recalculates these
// values from the selected resource when a booking is saved.
export function estimateRentalTax(startDate, endDate, resource) {
  return Math.max(0, nightsBetween(startDate, endDate)) * (Number(resource?.rentalTaxPerNight) || 0);
}

export function estimateBalance(charge, received, commission, rentalTax = 0) {
  return (Number(charge) || 0) - (Number(received) || 0) - (Number(commission) || 0) - (Number(rentalTax) || 0);
}

export function estimateRentalSubTotal(charge, rentalTax = 0) {
  return (Number(charge) || 0) - (Number(rentalTax) || 0);
}

export function hasAvailabilityConflict(candidate, bookings, ignoredBookingId = null) {
  if (!['active', 'paid'].includes(candidate.status)) return false;
  const candidateStart = europeanToIsoDate(candidate.startDate);
  const candidateEnd = europeanToIsoDate(candidate.endDate);
  if (!candidateStart || !candidateEnd || candidateStart >= candidateEnd) return false;

  return bookings.some((booking) => {
    if (booking.id === ignoredBookingId) return false;
    if (booking.resourceId !== candidate.resourceId || !['active', 'paid'].includes(booking.status)) {
      return false;
    }

    const bookingStart = europeanToIsoDate(booking.startDate);
    const bookingEnd = europeanToIsoDate(booking.endDate);
    return bookingStart && bookingEnd && candidateStart < bookingEnd && bookingStart < candidateEnd;
  });
}

export function toBookingPayload(form) {
  return {
    name: form.name.trim(),
    numOfGuests: Number(form.numOfGuests),
    agency: form.agency,
    resourceId: form.resourceId,
    country: form.country.trim(),
    startDate: isoToEuropeanDate(form.startDate),
    endDate: isoToEuropeanDate(form.endDate),
    charge: form.charge === '' ? null : Number(form.charge),
    commission: form.commission === '' ? null : Number(form.commission),
    received: form.received === '' ? null : Number(form.received),
    status: form.status,
    identification: form.identification.trim(),
    afm: form.afm.trim(),
    email: form.email.trim(),
    tel: form.tel.trim(),
    voucher: form.voucher.trim(),
    taxRefNumber: form.taxRefNumber.trim(),
    invoiceNumber: form.invoiceNumber.trim(),
    platformReservationId: (form.platformReservationId ?? '').trim(),
    extraInfo: form.extraInfo.trim(),
  };
}

export function bookingToForm(booking) {
  return {
    name: booking.name ?? '',
    numOfGuests: booking.numOfGuests == null ? '' : String(booking.numOfGuests),
    agency: booking.agency ?? '',
    resourceId: booking.resourceId ?? '',
    country: booking.country ?? '',
    startDate: europeanToIsoDate(booking.startDate),
    endDate: europeanToIsoDate(booking.endDate),
    charge: booking.charge == null ? '' : String(booking.charge),
    commission: booking.commission == null ? '' : String(booking.commission),
    received: booking.received == null ? '' : String(booking.received),
    status: booking.status ?? 'active',
    identification: booking.identification ?? '',
    afm: booking.afm ?? '',
    email: booking.email ?? '',
    tel: booking.tel ?? '',
    voucher: booking.voucher ?? '',
    taxRefNumber: booking.taxRefNumber ?? '',
    invoiceNumber: booking.invoiceNumber ?? '',
    platformReservationId: booking.platformReservationId ?? '',
    extraInfo: booking.extraInfo ?? '',
  };
}

export function toTimelineItem(booking, mode) {
  const safeName = escapeHtml(booking.name ?? '');
  const guestCount = booking.numOfGuests ?? '';
  const nights = booking.numOfNights ?? '';
  const className = [
    booking.agency,
    booking.status === 'cancelled' ? 'booking-cancelled' : '',
  ].filter(Boolean).join(' ');
  return {
    id: booking.id,
    type: 'range',
    group: booking.resourceId,
    start: localMidday(europeanToIsoDate(booking.startDate)),
    end: localMidday(europeanToIsoDate(booking.endDate)),
    content: `${mode.timelinePrefix}${nights} ${mode.timelineGuestLabel}${guestCount} ${safeName}`,
    className,
    recordData: booking,
  };
}

export function formatDateForApi(value) {
  return isoToEuropeanDate(value);
}

export function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character]);
}

function localMidday(isoDate) {
  if (!isoDate) return null;
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day, 12, 0, 0);
}
