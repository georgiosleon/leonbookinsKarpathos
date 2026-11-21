import { useEffect, useRef, useState } from 'react';
import { useLanguage } from '../i18n.jsx';

export function HelpButton() {
  const [open, setOpen] = useState(false);
  const [section, setSection] = useState('installation');
  const dialogRef = useRef(null);
  const closeButtonRef = useRef(null);
  const { t } = useLanguage();

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

  function handleTabKeyDown(event, nextSection) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
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
                onKeyDown={(event) => handleTabKeyDown(event, 'operations')}
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
                onKeyDown={(event) => handleTabKeyDown(event, 'installation')}
              >
                {t('Operations')}
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
                    <h3>{t('Install the portable app')}</h3>
                    <ol>
                      <li>{t('Download the portable ZIP for your operating system and processor from the project release. macOS builds are architecture-specific.')}</li>
                      <li>{t('Extract the ZIP to a folder you can write to. Keep the application folder together with its bundled runtime.')}</li>
                      <li>{t('Open a terminal in the extracted application folder. On Windows run .\\LeonBookings.exe; on macOS run ./LeonBookings.app/Contents/MacOS/LeonBookings.')}</li>
                      <li>{t('Enter the administrator credentials when prompted. Passwords must be 12–72 UTF-8 bytes. Keep the terminal open while using the app; press Enter there to stop it.')}</li>
                      <li>{t('The database is bookings.db in the launcher working directory. Start the app from its extracted folder to keep its database there. Logs are stored in your user application-data folder.')}</li>
                    </ol>
                    <p>{t('Portable builds include Java and do not need a separate Java installation. Unsigned downloads may show Windows SmartScreen or macOS Gatekeeper warnings.')}</p>
                  </section>

                  <section>
                    <h3>{t('Optional LAN access')}</h3>
                    <ol>
                      <li>{t('The desktop app and Docker setup are local-only by default. For desktop LAN access, set SERVER_ADDRESS=0.0.0.0 in the terminal before starting the launcher. For Docker Compose, set BOOKING_HOST_ADDRESS=0.0.0.0 in the private .env file and restart the service.')}</li>
                      <li>{t('Use the command for your platform below to start the desktop app with LAN access enabled. For Docker, edit BOOKING_HOST_ADDRESS in .env and run docker compose up -d.')}</li>
                      <li>{t('On another device connected to the same trusted network, open http:// followed by the host computer LAN IP and :8080. The host computer must remain on and the firewall must allow port 8080 only from trusted devices.')}</li>
                      <li>{t('LAN access uses the same single administrator account and database on the host computer. The app does not provide HTTPS; do not expose it directly to the internet. Use the encrypted backup process to share database data, not a live database file over a network share.')}</li>
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
                      <li>{t('The React frontend provides the booking form, timelines, reports, and resource screens. It communicates with the backend using same-origin HTTP and JSON.')}</li>
                      <li>{t('Spring Boot separates REST controllers and request validation, booking services and business rules, and repositories that persist data.')}</li>
                      <li>{t('SQLite stores bookings and the managed room/vehicle catalog. The desktop launcher starts the same Spring Boot application and opens it in your browser.')}</li>
                      <li>{t('Active/Paid overlap checks are enforced by the backend and have separate room and vehicle settings. Cancelled bookings are retained but do not reserve availability.')}</li>
                      <li>{t('The current installation uses one administrator configured at startup. User accounts and per-user permissions are not yet stored in the database.')}</li>
                      <li>{t('Database initialization is currently idempotent SQL. Versioned Liquibase migrations are planned for future schema evolution; no migration history is stored yet.')}</li>
                      <li>{t('The app uses one Spring Boot deployment and synchronous HTTP/JSON. Kafka and Protocol Buffers are not included because the current app has no separate event consumers or binary API requirement.')}</li>
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
                      <li>{t('Choose Rooms or Cars & bikes from the top navigation.')}</li>
                      <li>{t('Use the form to create a booking, or select a timeline item to load an existing booking.')}</li>
                      <li>{t('Select a booking on the timeline to review its details. Use the date controls to move the timeline.')}</li>
                      <li>{t('Use Reports to review bookings or print or save the filtered report as PDF. Use Manage rooms and cars to add resources.')}</li>
                    </ol>
                  </section>

                  <section>
                    <h3>{t('Creating, updating, and cancelling bookings')}</h3>
                    <p>{t('To create, enter the required fields and choose Save as new booking. To update or cancel an existing booking, select it on a timeline, enable Edit selected booking, make the changes, and choose Update booking. To cancel, set Status to Cancelled. Cancellation keeps the record; deleting removes it permanently.')}</p>
                  </section>

                  <section>
                    <h3>{t('Booking validations')}</h3>
                    <ul>
                      <li>{t('Name, guest count, agency, resource, arrival date, departure date, and status are required.')}</li>
                      <li>{t('The selected resource must be active and managed for the chosen booking type.')}</li>
                      <li>{t('Guest count must be a positive number. Arrival and departure must be valid dates, and departure must be after arrival.')}</li>
                      <li>{t('If provided, email must be valid. Charge, commission, and received amounts cannot be negative.')}</li>
                      <li>{t('Status must be Active, Paid, or Cancelled. Active/Paid date overlaps for the same resource are rejected when that resource-type rule is enabled.')}</li>
                      <li>{t('The backend checks these rules when saving. Cancelled bookings do not block availability; same-day checkout and next check-in are allowed.')}</li>
                    </ul>
                  </section>

                  <section>
                    <h3>{t('Prepare for an application update')}</h3>
                    <p>{t('Stop the app and create an encrypted backup before installing an update. Keep a copy outside the application folder and note the database path printed at startup. Run the new version against the same database path, then verify recent bookings and resources. Current startup SQL creates missing tables but does not migrate changed columns; test updates on a copy and keep the backup until verification is complete.')}</p>
                  </section>

                  <section>
                    <h3>{t('Back up the database')}</h3>
                    <ol>
                      <li>{t('The SQLite backup command can run while the app is open. Open a second terminal in the portable application folder. Keep the backup separate from the computer or folder containing the live database.')}</li>
                      <li>{t('The live database is bookings.db in the launcher working directory; use the database path printed at startup if yours differs. Replace the --database path in the command if necessary.')}</li>
                      <li>{t('Run the matching encrypted backup command below. It prompts you twice for a passphrase and creates a timestamped file under backups without overwriting existing files.')}</li>
                      <li>{t('Send the encrypted backup file to support and share its passphrase separately using a trusted channel. You need the same passphrase to restore it; it cannot be recovered if lost.')}</li>
                      <li>{t('Copy the resulting backups folder to secure storage, such as an external drive.')}</li>
                    </ol>
                    <h4>{t('Windows portable app')}</h4>
                    <pre><code>{backupCommands.windows}</code></pre>
                    <h4>{t('macOS portable app')}</h4>
                    <pre><code>{backupCommands.macos}</code></pre>
                    <p>{t('For Linux or a custom database location, use the encrypted database backup command documented in the project README, replacing the database path as needed.')}</p>
                  </section>

                  <section className="help-warning">
                    <h3>{t('Important')}</h3>
                    <p>{t('The backup is encrypted with AES-256-GCM. Keep the passphrase private and separate from the backup file. The app does not email or upload backups; send the encrypted file through your approved support channel. Do not delete the live database after making a backup; verify that the encrypted backup file exists and copy it to a safe location.')}</p>
                  </section>

                  <section>
                    <h3>{t('Restore an encrypted backup')}</h3>
                    <p>{t('Stop the application before restoring. Run db restore with the encrypted .sqlite3.enc file as --input from a terminal in the app folder. Confirm the restore when prompted, then enter the same passphrase. The restore process validates the backup and saves a pre-restore copy of the current database.')}</p>
                    <h4>{t('Windows portable app')}</h4>
                    <pre><code>{restoreCommands.windows}</code></pre>
                    <h4>{t('macOS portable app')}</h4>
                    <pre><code>{restoreCommands.macos}</code></pre>
                    <p>{t('Replace your-backup.sqlite3.enc with the actual backup filename. For Linux or a custom database location, see the backup and restore instructions in the project README.')}</p>
                  </section>
              </div>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
