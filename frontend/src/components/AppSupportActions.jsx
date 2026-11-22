import { useEffect, useState } from 'react';
import { fetchApplicationInfo, fetchCurrentUser } from '../bookingApi.js';
import { useLanguage } from '../i18n.jsx';

export function AppSupportActions() {
  const [version, setVersion] = useState('');
  const [isAdmin, setIsAdmin] = useState(false);
  const { t } = useLanguage();

  useEffect(() => {
    let active = true;
    fetchApplicationInfo()
      .then(({ version: applicationVersion }) => {
        if (active) setVersion(applicationVersion);
      })
      .catch(() => {
        if (active) setVersion(null);
      });
    fetchCurrentUser()
      .then((user) => {
        if (active) setIsAdmin(user?.role === 'ADMIN');
      })
      .catch(() => {
        if (active) setIsAdmin(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="app-support-actions">
      <span className="app-version">
        {version ? t('Version {version}', { version }) : t('Version unavailable')}
      </span>
      {isAdmin ? (
        <a className="button button-muted" href="/api/v1/diagnostics">
          {t('Export diagnostics')}
        </a>
      ) : null}
    </div>
  );
}
