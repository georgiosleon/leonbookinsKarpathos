const supportedBookingSaveModes = ['create', 'edit'];

export function resolveBookingSaveMode(value) {
  const mode = value || 'create';
  if (!supportedBookingSaveModes.includes(mode)) {
    throw new Error(
      `Invalid VITE_BOOKING_SAVE_MODE "${mode}". Use "create" or "edit".`,
    );
  }
  return mode;
}

export const defaultBookingSaveMode = resolveBookingSaveMode(
  import.meta.env?.VITE_BOOKING_SAVE_MODE,
);

const SAVE_MODE_SESSION_KEY = 'booking-save-mode';

export function getInitialBookingSaveMode() {
  const savedMode = window.sessionStorage.getItem(SAVE_MODE_SESSION_KEY);
  return resolveBookingSaveMode(savedMode || defaultBookingSaveMode);
}

export function storeBookingSaveMode(mode) {
  const saveMode = resolveBookingSaveMode(mode);
  window.sessionStorage.setItem(SAVE_MODE_SESSION_KEY, saveMode);
  return saveMode;
}
