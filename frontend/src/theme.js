const THEME_STORAGE_KEY = 'booking-theme';
const themes = ['light', 'dark'];

export function resolveTheme(value) {
  const theme = value || 'light';
  if (!themes.includes(theme)) {
    throw new Error(`Invalid theme "${theme}". Use "light" or "dark".`);
  }
  return theme;
}

export function getInitialTheme() {
  let savedTheme = null;
  try {
    savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  } catch {
    // Storage may be unavailable; fall back to the system preference.
  }
  if (savedTheme) {
    try {
      return resolveTheme(savedTheme);
    } catch {
      // Ignore stale values stored by an older version.
    }
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function storeTheme(theme) {
  const resolvedTheme = resolveTheme(theme);
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, resolvedTheme);
  } catch {
    // The selected theme still applies for this session.
  }
  return resolvedTheme;
}
