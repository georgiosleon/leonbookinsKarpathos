import { formatDateForApi } from './bookingLogic.js';
import { defaultLanguage, languageStorageKey, resolveLanguage } from './localeConfig.js';

let csrfToken;

async function request(url, options) {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Accept-Language': selectedLanguage(),
      ...options?.headers,
    },
  });
  if (response.status === 401) {
    window.location.assign('/login');
    throw new Error('Your session has expired. Please sign in again.');
  }
  if (!response.ok) {
    const message = (await response.text()).trim();
    throw new Error(message || `Request failed (${response.status}).`);
  }
  return response;
}

function selectedLanguage() {
  try {
    return resolveLanguage(window.localStorage.getItem(languageStorageKey));
  } catch {
    return defaultLanguage;
  }
}

async function csrfHeaders() {
  if (!csrfToken) {
    const response = await request('/api/v1/csrf');
    csrfToken = await response.json();
  }
  return { [csrfToken.headerName]: csrfToken.token };
}

export async function fetchBookings() {
  const response = await request('/api/v1/booking/getAll');
  return response.json();
}

export async function fetchResources() {
  const response = await request('/api/v1/resources');
  return response.json();
}

export async function fetchAvailabilitySettings() {
  const response = await request('/api/v1/settings/availability');
  return response.json();
}

export async function fetchApplicationInfo() {
  const response = await request('/api/v1/app-info');
  return response.json();
}

export async function createResource(resource) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/resources', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(resource),
  });
  return response.json();
}

export async function saveBooking(booking) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/booking/save', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(booking),
  });
  return response.json();
}

export async function updateBooking(booking) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/booking/update', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(booking),
  });
  return response.json();
}

export async function deleteBooking(id) {
  const csrf = await csrfHeaders();
  await request('/api/v1/booking/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify({ id }),
  });
}

export async function logout() {
  const csrf = await csrfHeaders();
  await request('/logout', { method: 'POST', headers: csrf });
  csrfToken = undefined;
  window.location.assign('/login?logout');
}

export async function fetchBookingReport(startDate, endDate, language = 'en') {
  const response = await request(bookingReportUrl('/api/v1/booking/report', startDate, endDate, language));
  return response.text();
}

export function bookingPdfUrl(startDate, endDate, language = 'en', type = 'rooms') {
  const query = new URLSearchParams({
    fromDate: formatDateForApi(startDate),
    toDate: formatDateForApi(endDate),
    lang: language,
    type,
  });
  return `/api/v1/booking/export/pdf?${query}`;
}

function bookingReportUrl(path, startDate, endDate, language) {
  const query = new URLSearchParams({
    fromDate: formatDateForApi(startDate),
    toDate: formatDateForApi(endDate),
    lang: language,
  });
  return `${path}?${query}`;
}
