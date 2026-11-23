import { useEffect, useState } from 'react';
import { isAuthRedirectError, logout } from '../../bookingApi.js';
import { createUser, deleteUser, fetchUsers, updateUser } from '../../usersApi.js';
import { useLanguage } from '../../i18n.jsx';
import { useTheme } from '../../useTheme.js';
import { FeedbackDialog } from '../FeedbackDialog.jsx';
import { AppHeader } from '../AppHeader.jsx';

const roleOptions = ['ADMIN', 'OPERATOR', 'GUEST'];

function emptyUser() {
  return { id: null, username: '', email: '', role: 'OPERATOR', active: true, password: '' };
}

export function UserManagement() {
  const { language, t } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(emptyUser());
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errorPopupOpen, setErrorPopupOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const isEditing = form.id != null;

  function showError(message) {
    setError(message);
    setErrorPopupOpen(Boolean(message));
  }

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  useEffect(() => {
    let active = true;
    fetchUsers()
      .then((data) => {
        if (active) setUsers(data);
      })
      .catch((requestError) => {
        if (active && !isAuthRedirectError(requestError)) {
          showError(t('Could not load users: {error}', { error: requestError.message }));
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [t]);

  function openEdit(user) {
    setForm({
      id: user.id,
      username: user.username,
      email: user.email || '',
      role: user.role || 'OPERATOR',
      active: user.active !== false,
      password: '',
    });
  }

  function updateForm(field) {
    return (event) => {
      const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
      setForm((current) => ({ ...current, [field]: value }));
    };
  }

  function setUsersFromSaved(savedUser, isEdit) {
    setUsers((current) => {
      const next = isEdit
        ? current.map((user) => (user.id === savedUser.id ? { ...user, ...savedUser } : user))
        : [...current, savedUser];
      return next.sort((left, right) => left.username.localeCompare(right.username));
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    showError('');
    setNotice('');
    setBusy(true);
    try {
      if (isEditing) {
        const saved = await updateUser(form);
        setUsersFromSaved(saved, true);
        setNotice(t('User saved.'));
      } else {
        const created = await createUser({
          username: form.username,
          email: form.email,
          role: form.role,
          active: form.active,
          password: form.password,
        });
        setUsersFromSaved(created, false);
        setNotice(t('User added.'));
      }
      setForm(emptyUser());
    } catch (requestError) {
      showError(t('Could not save user: {error}', { error: requestError.message }));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(user) {
    if (!window.confirm(t('Are you sure you want to delete {username}?', { username: user.username }))) {
      return;
    }
    showError('');
    try {
      await deleteUser(user.id);
      setUsers((current) => current.filter((u) => u.id !== user.id));
    } catch (requestError) {
      showError(t('Could not delete user: {error}', { error: requestError.message }));
    }
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (requestError) {
      showError(t('Could not sign out: {error}', { error: requestError.message }));
    }
  }

  return (
    <main className="app-shell">
      <AppHeader
        title={t('Manage users')}
        currentPage="users"
        isAdmin={true}
        canAccessReports={true}
        theme={theme}
        onToggleTheme={toggleTheme}
        onLogout={handleLogout}
      />

      {error && <p className="feedback feedback-error" role="alert">{error}</p>}
      {notice && <p className="feedback feedback-success" role="status">{notice}</p>}

      <section className="panel" aria-labelledby="user-form-heading">
        <div className="panel-heading">
          <div>
            <h2 id="user-form-heading">{isEditing ? t('Edit user') : t('Add user')}</h2>
            <p>{t('Manage user accounts.')}</p>
          </div>
        </div>

        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field">
            <span>{t('Username *')}</span>
            <input required value={form.username} onChange={updateForm('username')} />
          </label>
          <label className="field">
            <span>{t('Email')}</span>
            <input value={form.email} onChange={updateForm('email')} />
          </label>
          <label className="field">
            <span>{t('Role')}</span>
            <select value={form.role} onChange={updateForm('role')}>
              {roleOptions.map((role) => <option key={role} value={role}>{t(role)}</option>)}
            </select>
          </label>
          <label className="field">
            <span>{t('Active')}</span>
            <input type="checkbox" checked={form.active} onChange={updateForm('active')} />
          </label>
          <label className="field">
            <span>{isEditing ? t('New password (leave blank to keep)') : t('Password *')}</span>
            <input
              required={!isEditing}
              type="password"
              minLength={isEditing ? undefined : 8}
              value={form.password}
              onChange={updateForm('password')}
              autoComplete={isEditing ? 'new-password' : 'new-password'}
            />
          </label>
          <div className="form-actions">
            <button type="submit" className="button button-primary" disabled={busy}>
              {busy ? t('Saving…') : isEditing ? t('Save user') : t('Add user')}
            </button>
            {isEditing && (
              <button type="button" className="button button-muted" onClick={() => setForm(emptyUser())}>
                {t('Cancel')}
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="panel" aria-label={t('Users')}>
        {loading
          ? <p className="feedback" role="status">{t('Loading users…')}</p>
          : (
            <div className="report-table-wrap" role="region" aria-label={t('Users')} tabIndex="0">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>{t('Username')}</th>
                    <th>{t('Email')}</th>
                    <th>{t('Role')}</th>
                    <th>{t('Active')}</th>
                    <th>{t('Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id}>
                      <td>{user.username}</td>
                      <td>{user.email}</td>
                      <td>{user.role}</td>
                      <td>{user.active ? t('Active') : t('Inactive')}</td>
                      <td>
                        <button type="button" className="button button-muted" onClick={() => openEdit(user)}>
                          {t('Edit user')}
                        </button>
                        <button type="button" className="button button-muted" onClick={() => handleDelete(user)}>
                          {t('Delete user')}
                        </button>
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr><td colSpan="5" className="report-empty-row">{t('No users found.')}</td></tr>
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

export default UserManagement;
