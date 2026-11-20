import { useEffect, useMemo, useState } from 'react';
import { bookingModes } from '../bookingConfig.js';
import { fetchBookingReport, fetchBookings, bookingPdfUrl } from '../bookingApi.js';
import { selectBookingsForReport, summarizeBookings } from '../reportLogic.js';

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

function formatMoney(value) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'EUR',
  }).format(value);
}

function getMode() {
  const requestedMode = new URLSearchParams(window.location.search).get('type');
  return bookingModes[requestedMode] ?? bookingModes.rooms;
}

export function ReportsPage() {
  const mode = useMemo(getMode, []);
  const [bookings, setBookings] = useState([]);
  const [range, setRange] = useState(currentMonthRange);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reportHtml, setReportHtml] = useState('');

  useEffect(() => {
    let active = true;
    fetchBookings()
      .then((data) => {
        if (active) setBookings(data);
      })
      .catch((requestError) => {
        if (active) setError(`Could not load booking data: ${requestError.message}`);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

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
    setError('');
    if (!range.startDate || !range.endDate || range.startDate > range.endDate) {
      setError('Choose a valid date range.');
      return;
    }

    try {
      setReportHtml(await fetchBookingReport(range.startDate, range.endDate));
    } catch (requestError) {
      setError(`Could not load detailed report: ${requestError.message}`);
    }
  }

  const reportQueryIsValid = range.startDate && range.endDate && range.startDate <= range.endDate;
  const pdfUrl = reportQueryIsValid
    ? bookingPdfUrl(range.startDate, range.endDate)
    : undefined;

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Karpathos booking planner</p>
          <h1>Booking reports</h1>
        </div>
        <div className="header-meta">
          <nav aria-label="Main navigation">
            <a href={`/?type=${mode.id}`}>Bookings</a>
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
              <span>Reports</span>
            </a>
          </nav>
          <span>{mode.title}</span>
        </div>
      </header>

      <section className="panel" aria-labelledby="report-filters-heading">
        <div className="panel-heading">
          <div>
            <h2 id="report-filters-heading">Report period</h2>
            <p>Bookings are included when their arrival date falls within the selected period.</p>
          </div>
          <div className="report-actions">
            <button type="button" className="button button-muted" onClick={showDetailedReport} disabled={!reportQueryIsValid}>
              Detailed report
            </button>
            {pdfUrl && (
              <a className="button button-primary" href={pdfUrl} target="_blank" rel="noreferrer">
                Export PDF
              </a>
            )}
          </div>
        </div>

        <div className="report-filters">
          <label className="field">
            <span>From</span>
            <input
              type="date"
              value={range.startDate}
              onChange={(event) => updateRange('startDate', event.target.value)}
            />
          </label>
          <label className="field">
            <span>To</span>
            <input
              type="date"
              value={range.endDate}
              min={range.startDate || undefined}
              onChange={(event) => updateRange('endDate', event.target.value)}
            />
          </label>
        </div>

        {error && <p className="feedback feedback-error" role="alert">{error}</p>}
        {loading && <p className="feedback" role="status">Loading booking data…</p>}
      </section>

      {!loading && !error && (
        <>
          <section className="report-summary-grid" aria-label="Period totals">
            <SummaryCard label="Bookings" value={summary.totalBookings} detail={`${summary.activeBookings} active/paid`} />
            <SummaryCard label="Cancelled" value={summary.cancelledBookings} detail="Excluded from financial totals" />
            <SummaryCard label="Guests" value={summary.guests} detail="Active and paid bookings" />
            <SummaryCard label="Nights" value={summary.nights} detail="Active and paid bookings" />
            <SummaryCard label="Charge" value={formatMoney(summary.charge)} detail="Active and paid bookings" />
            <SummaryCard label="Received" value={formatMoney(summary.received)} detail="Active and paid bookings" />
            <SummaryCard label="Balance" value={formatMoney(summary.balance)} detail="Charge less received and commission" />
          </section>

          <section className="panel" aria-labelledby="resource-report-heading">
            <div className="panel-heading">
              <div>
                <h2 id="resource-report-heading">By {mode.resourceLabel.toLowerCase()}</h2>
                <p>Cancelled bookings are excluded from occupancy and financial totals.</p>
              </div>
            </div>
            <div className="report-table-wrap">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>{mode.resourceLabel}</th>
                    <th>Bookings</th>
                    <th>Guests</th>
                    <th>Nights</th>
                    <th>Charge</th>
                    <th>Received</th>
                    <th>Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.resources.map((resource) => (
                    <tr key={resource.id}>
                      <th scope="row">{resource.name}</th>
                      <td>{resource.bookings}</td>
                      <td>{resource.guests}</td>
                      <td>{resource.nights}</td>
                      <td>{formatMoney(resource.charge)}</td>
                      <td>{formatMoney(resource.received)}</td>
                      <td>{formatMoney(resource.balance)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="panel" aria-labelledby="booking-list-heading">
            <div className="panel-heading">
              <div>
                <h2 id="booking-list-heading">Bookings in period</h2>
                <p>{filteredBookings.length} record(s) by arrival date, including cancelled bookings.</p>
              </div>
            </div>
            <div className="report-table-wrap">
              <table className="report-table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Arrival</th>
                    <th>Departure</th>
                    <th>{mode.resourceLabel}</th>
                    <th>Name</th>
                    <th>Agency</th>
                    <th>Guests</th>
                    <th>Nights</th>
                    <th>Charge</th>
                    <th>Received</th>
                    <th>Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredBookings.map((booking) => (
                    <tr key={booking.id} className={booking.status === 'cancelled' ? 'report-row-cancelled' : undefined}>
                      <td>{booking.status}</td>
                      <td>{booking.startDate}</td>
                      <td>{booking.endDate}</td>
                      <td>{booking.room}</td>
                      <td>{booking.name}</td>
                      <td>{booking.agency}</td>
                      <td>{booking.numOfGuests ?? 0}</td>
                      <td>{booking.numOfNights ?? 0}</td>
                      <td>{formatMoney(Number(booking.charge) || 0)}</td>
                      <td>{formatMoney(Number(booking.received) || 0)}</td>
                      <td>{formatMoney(Number(booking.balance) || 0)}</td>
                    </tr>
                  ))}
                  {filteredBookings.length === 0 && (
                    <tr>
                      <td colSpan="11" className="report-empty-row">No bookings start in this period.</td>
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
              <h2 id="official-report-heading">Detailed report</h2>
              <button type="button" className="button button-muted" onClick={() => setReportHtml('')}>Close</button>
            </div>
            <iframe title="Detailed booking report" sandbox="allow-same-origin" srcDoc={reportHtml} />
          </section>
        </div>
      )}
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
