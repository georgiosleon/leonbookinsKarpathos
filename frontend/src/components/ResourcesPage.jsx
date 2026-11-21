import { useEffect, useState } from 'react';
import { createResource, fetchResources, logout } from '../bookingApi.js';
import { LanguageSelector } from './LanguageSelector.jsx';
import { useLanguage } from '../i18n.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';
import { useTheme } from '../useTheme.js';
import { FeedbackDialog } from './FeedbackDialog.jsx';
import { HelpButton } from './HelpButton.jsx';
import { AppSupportActions } from './AppSupportActions.jsx';

export function ResourcesPage() {
  const [resources, setResources] = useState([]);
  const [type, setType] = useState(
    new URLSearchParams(window.location.search).get('type') === 'cars' ? 'cars' : 'rooms',
  );
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errorPopupOpen, setErrorPopupOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const { language, t } = useLanguage();
  const { theme, toggleTheme } = useTheme();

  function showError(message) {
    setError(message);
    setErrorPopupOpen(Boolean(message));
  }

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  useEffect(() => {
    let active = true;
    fetchResources()
      .then((data) => {
        if (active) setResources(data);
      })
      .catch((requestError) => {
        if (active) showError(t('Could not load resources: {error}', { error: requestError.message }));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [t]);

  async function handleSubmit(event) {
    event.preventDefault();
    showError('');
    setNotice('');
    if (!name.trim()) {
      showError(t('Enter a resource name.'));
      return;
    }

    setBusy(true);
    try {
      const resource = await createResource({ type, name: name.trim() });
      setResources((current) => [...current, resource].sort(
        (left, right) => left.type.localeCompare(right.type) || left.name.localeCompare(right.name),
      ));
      setName('');
      setNotice(t('Resource added.'));
    } catch (requestError) {
      showError(t('Could not add resource: {error}', { error: requestError.message }));
    } finally {
      setBusy(false);
    }
  }

  async function handleLogout() {
    showError('');
    try {
      await logout();
    } catch (requestError) {
      showError(t('Could not sign out: {error}', { error: requestError.message }));
    }
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">{t('Karpathos booking planner')}</p>
          <h1>{t('Manage rooms and cars')}</h1>
        </div>
        <div className="header-meta">
          <nav aria-label={t('Main navigation')}>
            <a href="/?type=rooms">{t('Rooms')}</a>
            <a href="/?type=cars">{t('Cars & bikes')}</a>
            <a href="/?type=rooms&page=reports">{t('Reports')}</a>
            <a href="/?page=resources" aria-current="page">{t('Manage rooms and cars')}</a>
          </nav>
          <HelpButton />
          <LanguageSelector />
          <ThemeToggle theme={theme} onToggle={toggleTheme} />
          <AppSupportActions />
          <button type="button" className="button button-muted" onClick={handleLogout}>{t('Sign out')}</button>
        </div>
      </header>

      <section className="panel" aria-labelledby="resources-heading">
        <div className="panel-heading">
          <div>
            <h2 id="resources-heading">{t('Rooms and cars')}</h2>
            <p>{t('Add the rooms and vehicles that can be selected for bookings.')}</p>
          </div>
        </div>

        {error && <p className="feedback feedback-error" role="alert">{error}</p>}
        {notice && <p className="feedback feedback-success" role="status">{notice}</p>}

        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field">
            <span>{t('Resource type *')}</span>
            <select value={type} onChange={(event) => setType(event.target.value)}>
              <option value="rooms">{t('Rooms')}</option>
              <option value="cars">{t('Cars & bikes')}</option>
            </select>
          </label>
          <label className="field">
            <span>{t('Resource name *')}</span>
            <input
              required
              maxLength="80"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <div className="form-actions">
            <button type="submit" className="button button-primary" disabled={busy}>
              {busy ? t('Saving…') : t('Add resource')}
            </button>
          </div>
        </form>
      </section>

      <section className="panel" aria-label={t('Rooms and cars')}>
        {loading
          ? <p className="feedback" role="status">{t('Loading resources…')}</p>
          : (
            <div className="report-table-wrap" role="region" aria-label={t('Rooms and cars')} tabIndex="0">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>{t('Resource type')}</th>
                    <th>{t('Resource name')}</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((resource) => (
                    <tr key={resource.id}>
                      <td>{t(resource.type === 'rooms' ? 'Rooms' : 'Cars & bikes')}</td>
                      <td>{resource.name}</td>
                    </tr>
                  ))}
                  {resources.length === 0 && (
                    <tr><td colSpan="2" className="report-empty-row">{t('No resources have been added.')}</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
      </section>
      <FeedbackDialog
        message={errorPopupOpen ? error : ''}
        onClose={() => setErrorPopupOpen(false)}
      />
    </main>
  );
}
