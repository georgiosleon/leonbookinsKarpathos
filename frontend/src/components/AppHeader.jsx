import { useEffect, useState } from 'react';
import { LanguageSelector } from './LanguageSelector.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';
import { HelpButton } from './HelpButton.jsx';
import { AppSupportActions } from './AppSupportActions.jsx';
import { useLanguage } from '../i18n.jsx';

const NAV_ITEMS = [
  { key: 'rooms', emoji: '🛏️', labelKey: 'Rooms', href: () => '/?type=rooms' },
  { key: 'cars', emoji: '🚗', labelKey: 'Cars & bikes', href: () => '/?type=cars' },
  { key: 'reports', emoji: '📊', labelKey: 'Reports', href: (modeId) => `/?type=${modeId || 'rooms'}&page=reports`, reportsAccess: true },
  { key: 'resources', emoji: '🧰', labelKey: 'Manage rooms and cars', href: () => '/?page=resources', adminOnly: true },
  { key: 'users', emoji: '👥', labelKey: 'Manage users', href: () => '/?page=users', adminOnly: true },
];

function formatHeaderDate(date, locale) {
  return date.toLocaleDateString(locale, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' });
}

function formatClock(date, locale) {
  return date.toLocaleTimeString(locale, { hour: 'numeric', minute: '2-digit', second: '2-digit' });
}

export function AppHeader({
  title,
  currentPage,
  modeId = 'rooms',
  isAdmin = false,
  canAccessReports = isAdmin,
  signedInLabel = '',
  theme,
  onToggleTheme,
  onLogout,
  extraControls = null,
  currentDate = null,
  clock = null,
}) {
  const { locale, t } = useLanguage();
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  const resolvedCurrentDate = currentDate ?? formatHeaderDate(now, locale);
  const resolvedClock = clock ?? formatClock(now, locale);

  return (
    <header className="app-header app-header-command-bar">
      <div className="app-header-top">
        <div>
          <div className="app-brand-row">
            <p className="eyebrow">{t('Karpathos booking planner')}</p>
            <AppSupportActions showDiagnostics={false} className="app-brand-version" />
            <time className="header-date"><span aria-hidden="true">📅 </span>{resolvedCurrentDate}</time>
            <time className="header-clock"><span aria-hidden="true">🕒 </span>{resolvedClock}</time>
          </div>
          <h1>{title}</h1>
          {signedInLabel ? <p className="signed-in-label">{signedInLabel}</p> : null}
        </div>
        <div className="app-header-top-meta">
          {extraControls}
          <ThemeToggle theme={theme} onToggle={onToggleTheme} />
          <LanguageSelector />
          <HelpButton />
          <AppSupportActions showVersion={false} />
          <button type="button" className="button button-muted" onClick={onLogout}>
            <span aria-hidden="true">🚪 </span>{t('Sign out')}
          </button>
        </div>
      </div>

      <div className="app-command-bar" role="navigation" aria-label={t('Main navigation')}>
        <nav className="app-nav-tabs">
          {NAV_ITEMS.filter((item) => (!item.adminOnly || isAdmin) && (!item.reportsAccess || canAccessReports)).map((item) => {
            const current = item.key === currentPage;
            return (
              <a
                key={item.key}
                href={item.href(modeId)}
                className={`app-nav-tab${current ? ' is-current' : ''}`}
                aria-current={current ? 'page' : undefined}
              >
                <span aria-hidden="true" className="app-nav-emoji">{item.emoji}</span>
                <span>{t(item.labelKey)}</span>
              </a>
            );
          })}
        </nav>
      </div>
    </header>
  );
}
