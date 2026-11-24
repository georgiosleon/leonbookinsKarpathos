import { useEffect, useRef, useState } from 'react';
import { useLanguage } from '../i18n.jsx';

const HELP_SECTIONS = ['installation', 'operations', 'support'];

export function HelpButton() {
  const [open, setOpen] = useState(false);
  const [section, setSection] = useState('installation');
  const [diagnostics, setDiagnostics] = useState('');
  const [diagnosticsLoading, setDiagnosticsLoading] = useState(false);
  const [diagnosticsError, setDiagnosticsError] = useState('');
  const dialogRef = useRef(null);
  const closeButtonRef = useRef(null);
  const { t } = useLanguage();

  useEffect(() => {
    if (!open || section !== 'support' || diagnostics) return undefined;
    let active = true;
    setDiagnosticsLoading(true);
    setDiagnosticsError('');
    fetch('/api/v1/diagnostics')
      .then((response) => {
        if (!response.ok) throw new Error(`Request failed (${response.status}).`);
        return response.text();
      })
      .then((text) => {
        if (active) setDiagnostics(text);
      })
      .catch(() => {
        if (active) setDiagnosticsError(t('Could not load support information.'));
      })
      .finally(() => {
        if (active) setDiagnosticsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [open, section, diagnostics, t]);

  useEffect(() => {
    if (!open) return undefined;
    const previousFocus = document.activeElement;
    closeButtonRef.current?.focus();
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOpen(false);
      } else if (event.key === 'Tab') {
        const focusable = dialogRef.current?.querySelectorAll(
          'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        );
        if (!focusable?.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previousFocus?.focus?.();
    };
  }, [open]);

  const backupCommands = {
    windows: String.raw`.\runtime\bin\java.exe -jar .\app\leonbookinsKarpathos.jar db backup --database .\bookings.db --encrypt`,
    macos: './LeonBookings.app/Contents/runtime/Contents/Home/bin/java -jar ./LeonBookings.app/Contents/app/leonbookinsKarpathos.jar db backup --database ./bookings.db --encrypt',
  };
  const restoreCommands = {
    windows: String.raw`.\runtime\bin\java.exe -jar .\app\leonbookinsKarpathos.jar db restore --database .\bookings.db --input .\backups\your-backup.sqlite3.enc`,
    macos: './LeonBookings.app/Contents/runtime/Contents/Home/bin/java -jar ./LeonBookings.app/Contents/app/leonbookinsKarpathos.jar db restore --database ./bookings.db --input ./backups/your-backup.sqlite3.enc',
  };
  const lanCommands = {
    windows: String.raw`$env:SERVER_ADDRESS = "0.0.0.0"; .\LeonBookings.exe`,
    linux: 'SERVER_ADDRESS=0.0.0.0 ./bin/LeonBookings',
    macos: 'SERVER_ADDRESS=0.0.0.0 ./LeonBookings.app/Contents/MacOS/LeonBookings',
  };

  function handleTabKeyDown(event, currentSection) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    const currentIndex = HELP_SECTIONS.indexOf(currentSection);
    const delta = event.key === 'ArrowRight' ? 1 : -1;
    const nextIndex = (currentIndex + delta + HELP_SECTIONS.length) % HELP_SECTIONS.length;
    const nextSection = HELP_SECTIONS[nextIndex];
    setSection(nextSection);
    document.getElementById(`help-${nextSection}-tab`)?.focus();
  }

  return (
    <>
      <button type="button" className="button button-muted help-button" onClick={() => setOpen(true)}>
        {t('Help')}
      </button>
      {open && (
        <div className="dialog-backdrop help-backdrop" role="presentation">
          <section
            ref={dialogRef}
            className="help-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="help-dialog-heading"
          >
            <header className="help-dialog-heading">
              <div>
                <p className="eyebrow">{t('Leon Bookings')}</p>
                <h2 id="help-dialog-heading">{t('Application help')}</h2>
              </div>
              <button
                ref={closeButtonRef}
                type="button"
                className="button button-muted"
                onClick={() => setOpen(false)}
              >
                {t('Close')}
              </button>
            </header>

            <nav className="help-tabs" role="tablist" aria-label={t('Help sections')}>
              <button
                id="help-installation-tab"
                type="button"
                role="tab"
                tabIndex={section === 'installation' ? 0 : -1}
                aria-selected={section === 'installation'}
                aria-controls="help-installation"
                onClick={() => setSection('installation')}
                onKeyDown={(event) => handleTabKeyDown(event, 'installation')}
              >
                {t('Installation')}
              </button>
              <button
                id="help-operations-tab"
                type="button"
                role="tab"
                tabIndex={section === 'operations' ? 0 : -1}
                aria-selected={section === 'operations'}
                aria-controls="help-operations"
                onClick={() => setSection('operations')}
                onKeyDown={(event) => handleTabKeyDown(event, 'operations')}
              >
                {t('Operations')}
              </button>
              <button
                id="help-support-tab"
                type="button"
                role="tab"
                tabIndex={section === 'support' ? 0 : -1}
                aria-selected={section === 'support'}
                aria-controls="help-support"
                onClick={() => setSection('support')}
                onKeyDown={(event) => handleTabKeyDown(event, 'support')}
              >
                {t('Support')}
              </button>
            </nav>

            <div className="help-dialog-content">
              <div
                id="help-installation"
                role="tabpanel"
                aria-labelledby="help-installation-tab"
                hidden={section !== 'installation'}
              >
                <section>
                  <h3>{t('Install and start')}</h3>
                  <ol>
                    <li>{t('Download the portable ZIP or installer for your operating system from the project release. macOS builds are architecture-specific.')}</li>
                    <li>{t('Extract or install the app, then start it from a terminal in the application folder. Keep the bundled runtime with the app.')}</li>
                    <li>{t('Configure seed account credentials through environment variables before first startup (APP_ADMIN_*, APP_OPERATOR_*, APP_GUEST_*). Passwords must be at least 12 characters; the built-in default is rejected in production.')}</li>
                    <li>{t('Sign in with a configured account. The SQLite database is bookings.db in the launcher working directory unless configured otherwise.')}</li>
                  </ol>
                  <p>{t('Portable builds include Java and do not need a separate Java installation. Unsigned downloads may show Windows SmartScreen or macOS Gatekeeper warnings.')}</p>
                </section>

                <section>
                  <h3>{t('License activation')}</h3>
                  <p>{t('A fresh installation requires one-time ADMIN license activation before normal use. The activation screen shows the Installation ID; provide it to the license issuer, then paste the signed license key. Activation takes effect immediately.')}</p>
                </section>

                <section>
                  <h3>{t('Roles and access')}</h3>
                  <div className="report-table-wrap" role="region" aria-label={t('Roles and access')}>
                    <table className="report-table">
                      <thead>
                        <tr>
                          <th>{t('Capability')}</th>
                          <th>ADMIN</th>
                          <th>OPERATOR</th>
                          <th>GUEST</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr><td>{t('View all bookings')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td></tr>
                        <tr><td>{t('Create/edit/delete bookings')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td><td>{t('No')}</td></tr>
                        <tr><td>{t('View reports / export')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td><td>{t('No')}</td></tr>
                        <tr><td>{t('Manage rooms & cars')}</td><td>{t('Yes')}</td><td>{t('No')}</td><td>{t('No')}</td></tr>
                        <tr><td>{t('Manage users')}</td><td>{t('Yes')}</td><td>{t('No')}</td><td>{t('No')}</td></tr>
                        <tr><td>{t('Activate/manage license & customer')}</td><td>{t('Yes')}</td><td>{t('No')}</td><td>{t('No')}</td></tr>
                        <tr><td>{t('Support')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td><td>{t('Yes')}</td></tr>
                      </tbody>
                    </table>
                  </div>
                  <p>{t('The system prevents removing, deactivating, or deleting the last active ADMIN and the last active OPERATOR account.')}</p>
                </section>

                <section>
                  <h3>{t('Optional LAN access')}</h3>
                  <ol>
                    <li>{t('The app is local-only by default. For desktop LAN access, set SERVER_ADDRESS=0.0.0.0 before starting the launcher. For Docker Compose, set BOOKING_HOST_ADDRESS=0.0.0.0 in .env and restart.')}</li>
                    <li>{t('On another device on the same trusted network, open http://<host-lan-ip>:8080. The host must remain on and the firewall must allow port 8080 only from trusted devices.')}</li>
                    <li>{t('LAN access uses the same host database and user accounts. The app does not provide HTTPS; do not expose it directly to the internet. Use encrypted backups to share data, not a live database file over a network share.')}</li>
                  </ol>
                  <h4>{t('Windows portable app')}</h4>
                  <pre><code>{lanCommands.windows}</code></pre>
                  <h4>{t('Linux portable app')}</h4>
                  <pre><code>{lanCommands.linux}</code></pre>
                  <h4>{t('macOS portable app')}</h4>
                  <pre><code>{lanCommands.macos}</code></pre>
                </section>

                <section>
                  <h3>{t('Application analysis and design')}</h3>
                  <ul>
                    <li>{t('The React frontend provides booking, reports, resources, and user-management screens. It communicates with the backend using same-origin HTTP/JSON.')}</li>
                    <li>{t('Spring Boot separates REST controllers, service/business rules, and Spring Data JPA repositories. Security is database-backed with role-based authorization and CSRF protection for browser writes.')}</li>
                    <li>{t('SQLite stores users, bookings, the managed room/vehicle catalog, and licensing data. Flyway manages schema migrations and Hibernate validates the schema at startup; DatabaseInitializer seeds missing default accounts only.')}</li>
                    <li>{t('Active/Paid overlap checks are enforced by the backend and have separate room and vehicle settings. Cancelled bookings are retained but do not reserve availability.')}</li>
                  </ul>
                </section>
              </div>

              <div
                id="help-operations"
                role="tabpanel"
                aria-labelledby="help-operations-tab"
                hidden={section !== 'operations'}
              >
                <section>
                  <h3>{t('Basic usage')}</h3>
                  <ol>
                    <li>{t('Sign in with your assigned account. Available actions depend on your role.')}</li>
                    <li>{t('Choose Rooms or Cars & bikes. Select a timeline item to review details; operators can create, edit, or delete bookings.')}</li>
                    <li>{t('Use Reports to review/export bookings (ADMIN/OPERATOR). Use Manage rooms and cars and Manage users for admin operations.')}</li>
                  </ol>
                </section>

                <section>
                  <h3>{t('Creating, updating, and cancelling bookings')}</h3>
                  <p>{t('Operators can create bookings with required fields and Save as new booking. To update/cancel, select a booking, enable Edit selected booking, make changes, and choose Update booking. Set Status to Cancelled to keep the record without blocking availability; deleting removes it permanently.')}</p>
                </section>

                <section>
                  <h3>{t('Booking validations')}</h3>
                  <ul>
                    <li>{t('Name, guest count, agency, resource, arrival date, departure date, and status are required.')}</li>
                    <li>{t('The selected resource must be active and managed for the chosen booking type.')}</li>
                    <li>{t('Guest count must be a positive number. Arrival and departure must be valid dates, and departure must be after arrival.')}</li>
                    <li>{t('If provided, email must be valid. Charge, commission, and received amounts cannot be negative.')}</li>
                    <li>{t('Status must be Active, Paid, or Cancelled. Active/Paid date overlaps for the same resource are rejected when that resource-type rule is enabled.')}</li>
                  </ul>
                </section>

                <section>
                  <h3>{t('Prepare for an application update')}</h3>
                  <p>{t('Stop the app and create an encrypted backup before installing an update. Test the new version against a copy of the database, verify recent bookings/resources/users, then upgrade the live installation. Keep the backup until verification is complete.')}</p>
                </section>

                <section>
                  <h3>{t('Back up the database')}</h3>
                  <ol>
                    <li>{t('Stop or keep the app running as needed; SQLite backup can run while open. Use a separate terminal in the app folder and keep backups outside the live app folder.')}</li>
                    <li>{t('Run the encrypted backup command below. It prompts twice for a passphrase and creates a timestamped .sqlite3.enc file under backups.')}</li>
                    <li>{t('Store the encrypted backup and passphrase separately through a trusted channel. The passphrase cannot be recovered if lost.')}</li>
                  </ol>
                  <h4>{t('Windows portable app')}</h4>
                  <pre><code>{backupCommands.windows}</code></pre>
                  <h4>{t('macOS portable app')}</h4>
                  <pre><code>{backupCommands.macos}</code></pre>
                  <p>{t('For Linux or a custom database location, use the encrypted database backup command documented in the project README, replacing the database path as needed.')}</p>
                </section>

                <section className="help-warning">
                  <h3>{t('Important')}</h3>
                  <p>{t('The backup is encrypted with AES-256-GCM. Keep the passphrase private and separate from the backup file. The app does not email or upload backups. Verify the backup file exists and copy it to safe storage.')}</p>
                </section>

                <section>
                  <h3>{t('Restore an encrypted backup')}</h3>
                  <p>{t('Stop the application before restoring. Run db restore with the encrypted .sqlite3.enc file as --input. Confirm restore and enter the same passphrase. A pre-restore copy of the current database is saved.')}</p>
                  <h4>{t('Windows portable app')}</h4>
                  <pre><code>{restoreCommands.windows}</code></pre>
                  <h4>{t('macOS portable app')}</h4>
                  <pre><code>{restoreCommands.macos}</code></pre>
                  <p>{t('Replace your-backup.sqlite3.enc with the actual backup filename. For Linux or a custom database location, see the backup and restore instructions in the project README.')}</p>
                </section>
              </div>

              <div
                id="help-support"
                role="tabpanel"
                aria-labelledby="help-support-tab"
                hidden={section !== 'support'}
              >
                <section>
                  <h3>{t('Support')}</h3>
                  <p>{t('Diagnostic information for troubleshooting.')}</p>
                  {diagnosticsLoading ? <p role="status">{t('Loading...')}</p> : null}
                  {diagnosticsError ? <p className="feedback feedback-error" role="alert">{diagnosticsError}</p> : null}
                  {!diagnosticsLoading && !diagnosticsError && diagnostics ? (
                    <pre><code>{diagnostics}</code></pre>
                  ) : null}
                </section>
              </div>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
