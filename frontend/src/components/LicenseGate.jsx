import { useEffect, useState } from 'react';
import {
  activateLicense,
  createCustomer,
  deleteCustomer,
  fetchCustomers,
  fetchLicenseStatus,
  logout as logoutRequest,
  isAuthRedirectError,
  updateCustomer,
} from '../bookingApi.js';
import { useLanguage } from '../i18n.jsx';
import { LanguageSelector } from './LanguageSelector.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';
import { useTheme } from '../useTheme.js';

const emptyCustomer = { name: '', telephone: '', email: '' };

export function LicenseGate({ currentUser, onActivated }) {
  const { t } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const isAdmin = currentUser?.role === 'ADMIN';
  const [status, setStatus] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [customerForm, setCustomerForm] = useState(emptyCustomer);
  const [editingId, setEditingId] = useState(null);
  const [licenseKey, setLicenseKey] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function load() {
    const [nextStatus, nextCustomers] = await Promise.all([
      fetchLicenseStatus(),
      isAdmin ? fetchCustomers() : Promise.resolve([]),
    ]);
    setStatus(nextStatus);
    setCustomers(nextCustomers);
    if (isAdmin && nextCustomers.length > 0) {
      const first = nextCustomers[0];
      setEditingId(first.id);
      setCustomerForm({ name: first.name || '', telephone: first.telephone || '', email: first.email || '' });
    } else {
      setEditingId(null);
      setCustomerForm(emptyCustomer);
    }
  }

  useEffect(() => {
    let active = true;
    load().catch((e) => {
      if (active && !isAuthRedirectError(e)) setError(e.message);
    });
    return () => { active = false; };
  }, [isAdmin]);

  async function handleCustomerSave(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    setBusy(true);
    try {
      if (editingId) {
        await updateCustomer(editingId, customerForm);
        setNotice(t('Customer updated.'));
      } else {
        await createCustomer(customerForm);
        setNotice(t('Customer created.'));
      }
      await load();
    } catch (e) {
      if (!isAuthRedirectError(e)) setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleCustomerDelete() {
    if (!editingId) return;
    setError('');
    setNotice('');
    setBusy(true);
    try {
      await deleteCustomer(editingId);
      setNotice(t('Customer deleted.'));
      await load();
    } catch (e) {
      if (!isAuthRedirectError(e)) setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleActivate(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    setBusy(true);
    try {
      const nextStatus = await activateLicense(licenseKey);
      setStatus(nextStatus);
      setNotice(t('License activated successfully.'));
      setLicenseKey('');
      if (nextStatus.activated) onActivated(nextStatus);
    } catch (e) {
      if (!isAuthRedirectError(e)) setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleLogout() {
    try { await logoutRequest(); } catch (e) { if (!isAuthRedirectError(e)) setError(e.message); }
  }

  return (
    <main className="app-shell">
      <header className="app-header app-header-command-bar">
        <div className="app-header-top">
          <div>
            <p className="eyebrow">{t('Karpathos booking planner')}</p>
            <h1>{t('License activation')}</h1>
            {currentUser ? <p className="signed-in-label">{t('Signed in as {username}', { username: `${currentUser.username} (${currentUser.role})` })}</p> : null}
          </div>
          <div className="app-header-top-meta">
            <ThemeToggle theme={theme} onToggle={toggleTheme} />
            <LanguageSelector />
            <button type="button" className="button button-muted" onClick={handleLogout}>
              <span aria-hidden="true">🚪 </span>{t('Sign out')}
            </button>
          </div>
        </div>
      </header>

      <section className="panel" aria-labelledby="license-heading">
        <div className="panel-heading">
          <div>
            <h2 id="license-heading">{t('License status')}</h2>
            <p>{isAdmin
              ? t('This installation requires a valid license key before the application can be used.')
              : t('License activation required. Please contact an administrator.')}</p>
          </div>
        </div>

        {error && <p className="feedback feedback-error" role="alert">{error}</p>}
        {notice && <p className="feedback feedback-success" role="status">{notice}</p>}

        <div className="field">
          <label htmlFor="install-id">{t('Installation ID')}</label>
          <div className="toolbar">
            <input id="install-id" readOnly value={status?.installId || ''} />
            <button
              type="button"
              className="button button-muted"
              disabled={!status?.installId}
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(status.installId);
                  setNotice(t('Installation ID copied.'));
                } catch {
                  setError(t('Could not copy Installation ID.'));
                }
              }}
            >{t('Copy')}</button>
          </div>
          <p className="field-hint">{t('Provide this Installation ID to generate a license key.')}</p>
        </div>

        {status?.activated && status.customer ? (
          <p className="feedback feedback-success" role="status">
            {t('Activated for {name}', { name: status.customer.name })}
          </p>
        ) : null}
      </section>

      {isAdmin ? (
        <>
          <section className="panel" aria-labelledby="customer-heading">
            <div className="panel-heading">
              <div>
                <h2 id="customer-heading">{t('Customer')}</h2>
                <p>{t('Keep one customer record for this installation.')}</p>
              </div>
            </div>
            <form onSubmit={handleCustomerSave}>
              <div className="form-grid">
                <div className="field">
                  <label htmlFor="customer-name">{t('Name')}</label>
                  <input id="customer-name" required maxLength={255} value={customerForm.name}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, name: e.target.value }))} />
                </div>
                <div className="field">
                  <label htmlFor="customer-telephone">{t('Telephone')}</label>
                  <input id="customer-telephone" maxLength={100} value={customerForm.telephone}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, telephone: e.target.value }))} />
                </div>
                <div className="field">
                  <label htmlFor="customer-email">{t('Email')}</label>
                  <input id="customer-email" type="email" maxLength={255} value={customerForm.email}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, email: e.target.value }))} />
                </div>
              </div>
              <div className="toolbar">
                <button type="submit" className="button" disabled={busy}>{editingId ? t('Update customer') : t('Create customer')}</button>
                {editingId ? <button type="button" className="button button-muted" disabled={busy} onClick={handleCustomerDelete}>{t('Delete customer')}</button> : null}
              </div>
            </form>
          </section>

          <section className="panel" aria-labelledby="activate-heading">
            <div className="panel-heading">
              <div>
                <h2 id="activate-heading">{t('Activate license')}</h2>
                <p>{t('Paste the signed license key for this installation.')}</p>
              </div>
            </div>
            <form onSubmit={handleActivate}>
              <div className="field">
                <label htmlFor="license-key">{t('License key')}</label>
                <textarea id="license-key" rows={5} required value={licenseKey}
                  onChange={(e) => setLicenseKey(e.target.value)} />
              </div>
              <div className="toolbar">
                <button type="submit" className="button" disabled={busy}>{t('Activate')}</button>
              </div>
            </form>
          </section>
        </>
      ) : null}
    </main>
  );
}
