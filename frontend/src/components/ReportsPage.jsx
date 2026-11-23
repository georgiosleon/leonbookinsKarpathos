import { useEffect, useMemo, useState } from 'react';
import { bookingModes, resourcesForMode } from '../bookingConfig.js';
import {
  fetchBookingReport,
  fetchBookings,
  fetchResources,
  bookingPdfUrl,
  logout,
} from '../bookingApi.js';
import { selectBookingsForReport, summarizeBookings } from '../reportLogic.js';
import { ThemeToggle } from './ThemeToggle.jsx';
import { useTheme } from '../useTheme.js';
import { reportDocument } from '../reportDocument.js';
import { formatMoney, useLanguage } from '../i18n.jsx';
import { LanguageSelector } from './LanguageSelector.jsx';
import { FeedbackDialog } from './FeedbackDialog.jsx';
import { HelpButton } from './HelpButton.jsx';
import { AppSupportActions } from './AppSupportActions.jsx';
import { useCurrentUser } from '../useCurrentUser.js';

function currentMonthRange() {
  const today = new Date();
  const formatLocalDate = (date) => [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
  return {
    startDate: formatLocalDate(new Date(today.getFullYear(), today.getMonth(), 1)),
    endDate: formatLocalDate(new Date(today.getFullYear(), today.getMonth() + 1, 0)),
  };
}

function formatBookingDate(value, locale) {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value ?? '');
  if (!match) return value ?? '';
  const [, day, month, year] = match;
  return new Intl.DateTimeFormat(locale).format(new Date(Number(year), Number(month) - 1, Number(day)));
}

function getMode() {
  const requestedMode = new URLSearchParams(window.location.search).get('type');
  return bookingModes[requestedMode] ?? bookingModes.rooms;
}

export function ReportsPage() {
  const baseMode = useMemo(getMode, []);
  const [bookings, setBookings] = useState([]);
  const [resources, setResources] = useState([]);
  const [range, setRange] = useState(currentMonthRange);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [errorPopupOpen, setErrorPopupOpen] = useState(false);
  const [reportHtml, setReportHtml] = useState('');
  const { theme, toggleTheme } = useTheme();
  const { language, locale, t } = useLanguage();
  const { currentUser, loading: userLoading } = useCurrentUser();
  const isAdmin = currentUser?.role === 'ADMIN';
  const canAccessReports = isAdmin || currentUser?.role === 'OPERATOR';

  function showError(message) {
    setError(message);
    setErrorPopupOpen(Boolean(message));
  }

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  useEffect(() => {
    if (userLoading) return undefined;
    if (!canAccessReports) {
      setLoading(false);
      return undefined;
    }
    let active = true;
    Promise.all([fetchBookings(), fetchResources()])
      .then(([data, resourceData]) => {
        if (active) {
          setBookings(data);
          setResources(resourceData);
        }
      })
      .catch((requestError) => {
        if (active) showError(t('Could not load booking data: {error}', { error: requestError.message }));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [t, userLoading, canAccessReports]);

  const mode = useMemo(() => ({
    ...baseMode,
    resources: resourcesForMode(resources, baseMode),
  }), [baseMode, resources]);

  const filteredBookings = useMemo(
    () => selectBookingsForReport(bookings, mode, range.startDate, range.endDate),
    [bookings, mode, range],
  );
  const summary = useMemo(
    () => summarizeBookings(filteredBookings, mode),
    [filteredBookings, mode],
  );

  function updateRange(field, value) {
    setRange((current) => ({ ...current, [field]: value }));
  }

  async function showDetailedReport() {
    showError('');
    if (!range.startDate || !range.endDate || range.startDate > range.endDate) {
      showError(t('Choose a valid date range.'));
      return;
    }

    try {
      setReportHtml(await fetchBookingReport(range.startDate, range.endDate, language));
    } catch (requestError) {
      showError(t('Could not load detailed report: {error}', { error: requestError.message }));
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

  const reportQueryIsValid = range.startDate && range.endDate && range.startDate <= range.endDate;
  const pdfUrl = reportQueryIsValid
    ? bookingPdfUrl(range.startDate, range.endDate, language, mode.id)
    : undefined;

  if (userLoading) {
    return (
      <main className="app-shell">
        <p className="feedback" role="status">{t('Loading booking data…')}</p>
      </main>
    );
  }

  if (!canAccessReports) {
    return (
      <main className="app-shell">
        <header className="app-header">
          <div>
            <p className="eyebrow">{t('Karpathos booking planner')}</p>
            <h1>{t('Booking reports')}</h1>
          </div>
        </header>
        <p className="feedback feedback-error" role="alert">{t('You do not have permission to perform this action.')}</p>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">{t('Karpathos booking planner')}</p>
          <h1>{t('Booking reports')}</h1>
        </div>
        <div className="header-meta">
          <nav aria-label={t('Main navigation')}>
            <a href={`/?type=${mode.id}`}>{t('Bookings')}</a>
            <a className="nav-icon-link" href={`/?type=${mode.id}&page=reports`} aria-current="page">
              <svg
                className="nav-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M4 19.5h16" />
                <path d="M7 16V10" />
                <path d="M12 16V5" />
                <path d="M17 16v-4" />
              </svg>
              <span>{t('Reports')}</span>
            </a>
            {isAdmin && <a href="/?page=resources">{t('Manage rooms and cars')}</a>}
            {isAdmin && <a href="/?page=users">{t('Manage users')}</a>}
          </nav>
          <span>{t(mode.title)}</span>
          <HelpButton />
          <LanguageSelector />
          <ThemeToggle theme={theme} onToggle={toggleTheme} />
          <AppSupportActions />
          <button type="button" className="button button-muted" onClick={handleLogout}>{t('Sign out')}</button>
        </div>
      </header>

      <section className="panel" aria-labelledby="report-filters-heading">
        <div className="panel-heading">
          <div>
            <h2 id="report-filters-heading">{t('Report period')}</h2>
            <p>{t('Bookings are included when their arrival date falls within the selected period.')}</p>
          </div>
          <div className="report-actions">
            <button type="button" className="button button-muted" onClick={showDetailedReport} disabled={!reportQueryIsValid}>
              {t('Detailed report')}
            </button>
            {pdfUrl && (
              <a className="button button-primary" href={pdfUrl}>
                {t('Export PDF')}
              </a>
            )}
          </div>
        </div>

        <div className="report-filters">
          <label className="field">
            <span>{t('From')}</span>
            <input
              type="date"
              value={range.startDate}
              onChange={(event) => updateRange('startDate', event.target.value)}
            />
          </label>
          <label className="field">
            <span>{t('To')}</span>
            <input
              type="date"
              value={range.endDate}
              min={range.startDate || undefined}
              onChange={(event) => updateRange('endDate', event.target.value)}
            />
          </label>
        </div>

        {loading && <p className="feedback" role="status">{t('Loading booking data…')}</p>}
      </section>

      {!loading && !error && (
        <>
          <section className="report-summary-grid" aria-label={t('Period totals')}>
            <SummaryCard label={t('Bookings')} value={summary.totalBookings} detail={t('{count} active/paid bookings', { count: summary.activeBookings })} />
            <SummaryCard label={t('Cancelled bookings')} value={summary.cancelledBookings} detail={t('Excluded from financial totals')} />
            <SummaryCard label={t('Guests')} value={summary.guests} detail={t('Active and paid bookings')} />
            <SummaryCard label={t('Nights')} value={summary.nights} detail={t('Active and paid bookings')} />
            <SummaryCard label={t('Charge')} value={formatMoney(summary.charge, locale)} detail={t('Active and paid bookings')} />
            <SummaryCard label={t('Received')} value={formatMoney(summary.received, locale)} detail={t('Active and paid bookings')} />
            <SummaryCard label={t('Balance')} value={formatMoney(summary.balance, locale)} detail={t('Charge less received and commission')} />
          </section>

          <section className="panel" aria-labelledby="resource-report-heading">
            <div className="panel-heading">
              <div>
                <h2 id="resource-report-heading">{t('By {resource}', { resource: t(mode.resourceLabel).toLocaleLowerCase(locale) })}</h2>
                <p>{t('Cancelled bookings are excluded from occupancy and financial totals.')}</p>
              </div>
            </div>
            <div className="report-table-wrap" role="region" aria-label={t('{resource} summary table', { resource: t(mode.resourceLabel) })} tabIndex="0">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>{t(mode.resourceLabel)}</th>
                    <th>{t('Bookings')}</th>
                    <th>{t('Guests')}</th>
                    <th>{t('Nights')}</th>
                    <th>{t('Charge')}</th>
                    <th>{t('Received')}</th>
                    <th>{t('Balance')}</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.resources.map((resource) => (
                    <tr key={resource.id}>
                      <th scope="row">{resource.name}</th>
                      <td>{resource.bookings}</td>
                      <td>{resource.guests}</td>
                      <td>{resource.nights}</td>
                      <td>{formatMoney(resource.charge, locale)}</td>
                      <td>{formatMoney(resource.received, locale)}</td>
                      <td>{formatMoney(resource.balance, locale)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="panel" aria-labelledby="booking-list-heading">
            <div className="panel-heading">
              <div>
                <h2 id="booking-list-heading">{t('Bookings in period')}</h2>
                <p>{filteredBookings.length === 1
                  ? t('1 record by arrival date, including cancelled bookings.')
                  : t('{count} records by arrival date, including cancelled bookings.', { count: filteredBookings.length })}</p>
              </div>
            </div>
            <div className="report-table-wrap" role="region" aria-label={t('Bookings report table')} tabIndex="0">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>{t('Status')}</th>
                    <th>{t('Arrival')}</th>
                    <th>{t('Departure')}</th>
                    <th>{t(mode.resourceLabel)}</th>
                    <th>{t('Name')}</th>
                    <th>{t('Agency')}</th>
                    <th>{t('Guests')}</th>
                    <th>{t('Nights')}</th>
                    <th>{t('Charge')}</th>
                    <th>{t('Received')}</th>
                    <th>{t('Balance')}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredBookings.map((booking) => (
                    <tr key={booking.id} className={booking.status === 'cancelled' ? 'report-row-cancelled' : undefined}>
                      <td>{t(booking.status)}</td>
                      <td>{formatBookingDate(booking.startDate, locale)}</td>
                      <td>{formatBookingDate(booking.endDate, locale)}</td>
                      <td>{booking.resourceName ?? booking.resourceId}</td>
                      <td>{booking.name}</td>
                      <td>{t(mode.agencies.find((agency) => agency.id === booking.agency)?.text ?? booking.agency)}</td>
                      <td>{booking.numOfGuests ?? 0}</td>
                      <td>{booking.numOfNights ?? 0}</td>
                      <td>{formatMoney(Number(booking.charge) || 0, locale)}</td>
                      <td>{formatMoney(Number(booking.received) || 0, locale)}</td>
                      <td>{formatMoney(Number(booking.balance) || 0, locale)}</td>
                    </tr>
                  ))}
                  {filteredBookings.length === 0 && (
                    <tr>
                      <td colSpan="11" className="report-empty-row">{t('No bookings start in this period.')}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}

      {reportHtml && (
        <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setReportHtml('');
        }}>
          <section className="report-dialog" role="dialog" aria-modal="true" aria-labelledby="official-report-heading">
            <div className="report-heading">
              <h2 id="official-report-heading">{t('Detailed report')}</h2>
              <button type="button" className="button button-muted" onClick={() => setReportHtml('')}>{t('Close')}</button>
            </div>
            <iframe title={t('Detailed report')} sandbox="allow-same-origin" srcDoc={reportDocument(reportHtml, theme, language)} />
          </section>
        </div>
      )}
      {error && <p className="feedback feedback-error" role="alert">{error}</p>}
      <FeedbackDialog
        message={errorPopupOpen ? error : ''}
        onClose={() => setErrorPopupOpen(false)}
      />
    </main>
  );
}

function SummaryCard({ label, value, detail }) {
  return (
    <article className="report-summary-card">
      <h2>{label}</h2>
      <strong>{value}</strong>
      <p>{detail}</p>
    </article>
  );
}
