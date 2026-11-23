import { useEffect, useState } from 'react';
import { createResource, fetchResources, isAuthRedirectError, logout, updateResource } from '../bookingApi.js';
import { useLanguage } from '../i18n.jsx';
import { useTheme } from '../useTheme.js';
import { FeedbackDialog } from './FeedbackDialog.jsx';
import { AppHeader } from './AppHeader.jsx';
import { useCurrentUser } from '../useCurrentUser.js';

export function ResourcesPage() {
  const [resources, setResources] = useState([]);
  const [type, setType] = useState(
    new URLSearchParams(window.location.search).get('type') === 'cars' ? 'cars' : 'rooms',
  );
  const [name, setName] = useState('');
  const [rentalTaxPerNight, setRentalTaxPerNight] = useState('0');
  const [registrationNumber, setRegistrationNumber] = useState('');
  const [propertyAddress, setPropertyAddress] = useState('');
  const [taxCategory, setTaxCategory] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState({ name: '', rentalTaxPerNight: '0', active: true, registrationNumber: '', propertyAddress: '', taxCategory: '' });
  const [error, setError] = useState('');
  const [errorPopupOpen, setErrorPopupOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const { language, t } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const { currentUser, loading: userLoading } = useCurrentUser();
  const isAdmin = currentUser?.role === 'ADMIN';

  function showError(message) {
    setError(message);
    setErrorPopupOpen(Boolean(message));
  }

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  useEffect(() => {
    if (userLoading) return undefined;
    if (!isAdmin) {
      setLoading(false);
      return undefined;
    }
    let active = true;
    fetchResources()
      .then((data) => {
        if (active) setResources(data);
      })
      .catch((requestError) => {
        if (active && !isAuthRedirectError(requestError)) {
          showError(t('Could not load resources: {error}', { error: requestError.message }));
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [t, userLoading, isAdmin]);

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
      const resource = await createResource({ type, name: name.trim(), rentalTaxPerNight: rentalTaxPerNight === '' ? 0 : Number(rentalTaxPerNight), registrationNumber: registrationNumber.trim(), propertyAddress: propertyAddress.trim(), taxCategory: taxCategory.trim() });
      setResources((current) => [...current, resource].sort(
        (left, right) => left.type.localeCompare(right.type) || left.name.localeCompare(right.name),
      ));
      setName('');
      setRentalTaxPerNight('0');
      setRegistrationNumber('');
      setPropertyAddress('');
      setTaxCategory('');
      setNotice(t('Resource added.'));
    } catch (requestError) {
      showError(t('Could not add resource: {error}', { error: requestError.message }));
    } finally {
      setBusy(false);
    }
  }

  function startEdit(resource) {
    showError('');
    setNotice('');
    setEditingId(resource.id);
    setDraft({
      name: resource.name ?? '',
      rentalTaxPerNight: resource.rentalTaxPerNight == null ? '0' : String(resource.rentalTaxPerNight),
      active: resource.active !== false,
      registrationNumber: resource.registrationNumber ?? '',
      propertyAddress: resource.propertyAddress ?? '',
      taxCategory: resource.taxCategory ?? '',
    });
  }

  function cancelEdit() {
    setEditingId(null);
  }

  async function handleSaveEdit(resource) {
    showError('');
    setNotice('');
    if (!draft.name.trim()) {
      showError(t('Enter a resource name.'));
      return;
    }
    setBusy(true);
    try {
      const updated = await updateResource(resource.id, {
        name: draft.name.trim(),
        rentalTaxPerNight: draft.rentalTaxPerNight === '' ? 0 : Number(draft.rentalTaxPerNight),
        active: Boolean(draft.active),
        registrationNumber: (draft.registrationNumber ?? '').trim(),
        propertyAddress: (draft.propertyAddress ?? '').trim(),
        taxCategory: (draft.taxCategory ?? '').trim(),
      });
      setResources((current) => current
        .map((item) => (item.id === resource.id ? updated : item))
        .sort((left, right) => left.type.localeCompare(right.type) || left.name.localeCompare(right.name)));
      setEditingId(null);
      setNotice(t('Resource updated.'));
    } catch (requestError) {
      showError(t('Could not update resource: {error}', { error: requestError.message }));
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

  if (userLoading) {
    return (
      <main className="app-shell">
        <p className="feedback" role="status">{t('Loading resources…')}</p>
      </main>
    );
  }

  if (!isAdmin) {
    return (
      <main className="app-shell">
        <AppHeader
          title={t('Manage rooms and cars')}
          currentPage="resources"
          isAdmin={false}
          canAccessReports={false}
          theme={theme}
          onToggleTheme={toggleTheme}
          onLogout={handleLogout}
        />
        <p className="feedback feedback-error" role="alert">{t('You do not have permission to perform this action.')}</p>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <AppHeader
        title={t('Manage rooms and cars')}
        currentPage="resources"
        isAdmin={isAdmin}
        canAccessReports={isAdmin}
        theme={theme}
        onToggleTheme={toggleTheme}
        onLogout={handleLogout}
      />

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
          <label className="field">
            <span>{t('Rental tax per night (€)')}</span>
            <input
              type="number"
              min="0"
              step="0.01"
              value={rentalTaxPerNight}
              onChange={(event) => setRentalTaxPerNight(event.target.value)}
            />
          </label>
          <label className="field">
            <span>{t('Registration number')}</span>
            <input maxLength="120" value={registrationNumber} onChange={(event) => setRegistrationNumber(event.target.value)} />
          </label>
          <label className="field">
            <span>{t('Property address')}</span>
            <input maxLength="255" value={propertyAddress} onChange={(event) => setPropertyAddress(event.target.value)} />
          </label>
          <label className="field">
            <span>{t('Tax category')}</span>
            <input maxLength="120" value={taxCategory} onChange={(event) => setTaxCategory(event.target.value)} placeholder={t('e.g. Climate resilience fee')} />
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
                    <th>{t('Rental tax per night (€)')}</th>
                    <th>{t('Registration number')}</th>
                    <th>{t('Property address')}</th>
                    <th>{t('Tax category')}</th>
                    <th>{t('Active')}</th>
                    <th>{t('Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((resource) => {
                    const editing = editingId === resource.id;
                    return (
                      <tr key={resource.id}>
                        <td>{t(resource.type === 'rooms' ? 'Rooms' : 'Cars & bikes')}</td>
                        <td>
                          {editing ? (
                            <input
                              value={draft.name}
                              maxLength="80"
                              onChange={(event) => setDraft((d) => ({ ...d, name: event.target.value }))}
                            />
                          ) : resource.name}
                        </td>
                        <td>
                          {editing ? (
                            <input
                              type="number"
                              min="0"
                              step="0.01"
                              value={draft.rentalTaxPerNight}
                              onChange={(event) => setDraft((d) => ({ ...d, rentalTaxPerNight: event.target.value }))}
                            />
                          ) : Number(resource.rentalTaxPerNight ?? 0).toFixed(2)}
                        </td>
                        <td>{editing ? <input value={draft.registrationNumber} maxLength="120" onChange={(event) => setDraft((d) => ({ ...d, registrationNumber: event.target.value }))} /> : (resource.registrationNumber || '—')}</td>
                        <td>{editing ? <input value={draft.propertyAddress} maxLength="255" onChange={(event) => setDraft((d) => ({ ...d, propertyAddress: event.target.value }))} /> : (resource.propertyAddress || '—')}</td>
                        <td>{editing ? <input value={draft.taxCategory} maxLength="120" onChange={(event) => setDraft((d) => ({ ...d, taxCategory: event.target.value }))} /> : (resource.taxCategory || '—')}</td>
                        <td>
                          {editing ? (
                            <input
                              type="checkbox"
                              checked={Boolean(draft.active)}
                              onChange={(event) => setDraft((d) => ({ ...d, active: event.target.checked }))}
                            />
                          ) : (resource.active ? t('Yes') : t('No'))}
                        </td>
                        <td>
                          {editing ? (
                            <div className="form-actions" style={{ gap: '0.5rem' }}>
                              <button type="button" className="button button-primary" disabled={busy} onClick={() => handleSaveEdit(resource)}>{t('Save')}</button>
                              <button type="button" className="button button-muted" disabled={busy} onClick={cancelEdit}>{t('Cancel')}</button>
                            </div>
                          ) : (
                            <button type="button" className="button button-muted" onClick={() => startEdit(resource)}>{t('Edit')}</button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {resources.length === 0 && (
                    <tr><td colSpan="8" className="report-empty-row">{t('No resources have been added.')}</td></tr>
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
