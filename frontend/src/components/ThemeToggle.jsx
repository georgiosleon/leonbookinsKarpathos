import { useLanguage } from '../i18n.jsx';

export function ThemeToggle({ theme, onToggle }) {
  const nextTheme = theme === 'dark' ? 'light' : 'dark';
  const { t } = useLanguage();

  return (
    <button
      type="button"
      className="button button-muted theme-toggle"
      aria-label={t('Switch to {theme} theme', { theme: t(nextTheme === 'light' ? 'Light theme' : 'Dark theme') })}
      aria-pressed={theme === 'dark'}
      onClick={onToggle}
    >
      <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span>
      <span>{t(theme === 'dark' ? 'Light theme' : 'Dark theme')}</span>
    </button>
  );
}
