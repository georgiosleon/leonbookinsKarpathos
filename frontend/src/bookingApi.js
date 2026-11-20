import { formatDateForApi } from './bookingLogic.js';

async function request(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const message = (await response.text()).trim();
    throw new Error(message || `Request failed (${response.status}).`);
  }
  return response;
}

export async function fetchBookings() {
  const response = await request('/booking/getAll');
  return response.json();
}

export async function saveBooking(booking) {
  const response = await request('/booking/save', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(booking),
  });
  return response.json();
}

export async function updateBooking(booking) {
  const response = await request('/booking/update', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(booking),
  });
  return response.json();
}

export async function deleteBooking(id) {
  await request('/booking/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id }),
  });
}

export async function fetchBookingReport(startDate, endDate) {
  const response = await request(bookingReportUrl('/booking/report', startDate, endDate));
  return response.text();
}

export function bookingPdfUrl(startDate, endDate) {
  return bookingReportUrl('/booking/export/pdf', startDate, endDate);
}

function bookingReportUrl(path, startDate, endDate) {
  const query = new URLSearchParams({
    fromDate: formatDateForApi(startDate),
    toDate: formatDateForApi(endDate),
  });
  return `${path}?${query}`;
}
