import { formatDateForApi } from './bookingLogic.js';
import { defaultLanguage, languageStorageKey, resolveLanguage } from './localeConfig.js';

let csrfToken;
let redirectingToLogin = false;

export function isAuthRedirectError(error) {
  return error?.name === 'AuthRedirectError';
}

function authRedirectError() {
  const error = new Error('Authentication required.');
  error.name = 'AuthRedirectError';
  return error;
}

export async function request(url, options) {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Accept-Language': selectedLanguage(),
      ...options?.headers,
    },
  });
  if (response.status === 401) {
    if (!redirectingToLogin && !window.location.pathname.startsWith('/login')) {
      redirectingToLogin = true;
      window.location.assign('/login');
    }
    throw authRedirectError();
  }
  if (response.status === 403) {
    const message = (await response.text()).trim();
    throw new Error(message || 'You do not have permission to perform this action.');
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

export async function csrfHeaders() {
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

export async function fetchCurrentUser() {
  const response = await request('/api/v1/users/me');
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

export async function updateResource(id, resource) {
  const csrf = await csrfHeaders();
  const response = await request(`/api/v1/resources/${id}`, {
    method: 'PUT',
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

export async function fetchLicenseStatus() {
  const response = await request('/api/v1/license/status');
  return response.json();
}

export async function activateLicense(licenseKey) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/license/activate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify({ licenseKey }),
  });
  return response.json();
}

export async function fetchCustomers() {
  const response = await request('/api/v1/customers');
  return response.json();
}

export async function createCustomer(customer) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/customers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(customer),
  });
  return response.json();
}

export async function updateCustomer(id, customer) {
  const csrf = await csrfHeaders();
  const response = await request(`/api/v1/customers/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(customer),
  });
  return response.json();
}

export async function deleteCustomer(id) {
  const csrf = await csrfHeaders();
  await request(`/api/v1/customers/${id}`, {
    method: 'DELETE',
    headers: csrf,
  });
}
