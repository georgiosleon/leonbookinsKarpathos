import { useEffect, useState } from 'react';
import { fetchApplicationInfo } from '../bookingApi.js';
import { useLanguage } from '../i18n.jsx';

export function AppSupportActions({ showVersion = true, showDiagnostics = true, showLicense = true, className = '' }) {
  const [version, setVersion] = useState('');
  const [licenseActivated, setLicenseActivated] = useState(null);
  const { t } = useLanguage();

  useEffect(() => {
    let active = true;
    fetchApplicationInfo()
      .then((info) => {
        if (active) {
          setVersion(info.version);
          setLicenseActivated(info.licenseActivated);
        }
      })
      .catch(() => {
        if (active) setVersion(null);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className={`app-support-actions ${className}`.trim()}>
      {showVersion ? (
        <span className="app-version">
          {version ? t('Version {version}', { version }) : t('Version unavailable')}
          {showLicense && licenseActivated !== null ? ` • ${licenseActivated ? t('Licensed') : t('Not licensed')}` : ''}
        </span>
      ) : null}
      {showDiagnostics ? (
        <a className="button button-muted" href="/api/v1/support-bundle">
          {t('Download support bundle')}
        </a>
      ) : null}
    </div>
  );
}
