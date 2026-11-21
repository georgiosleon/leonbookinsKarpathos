import { useEffect, useState } from 'react';
import { fetchApplicationInfo } from '../bookingApi.js';
import { useLanguage } from '../i18n.jsx';

export function AppSupportActions() {
  const [version, setVersion] = useState('');
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
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="app-support-actions">
      <span className="app-version">
        {version ? t('Version {version}', { version }) : t('Version unavailable')}
      </span>
      <a className="button button-muted" href="/api/v1/diagnostics">
        {t('Export diagnostics')}
      </a>
    </div>
  );
}
