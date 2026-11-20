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
  const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (savedTheme) return resolveTheme(savedTheme);
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function storeTheme(theme) {
  const resolvedTheme = resolveTheme(theme);
  window.localStorage.setItem(THEME_STORAGE_KEY, resolvedTheme);
  return resolvedTheme;
}
