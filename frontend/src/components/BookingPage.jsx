import { useEffect, useMemo, useRef, useState } from 'react';
import { bookingModes, isBookableStatus, resourcesForMode } from '../bookingConfig.js';
import {
  getInitialBookingSaveMode,
  storeBookingSaveMode,
} from '../appConfig.js';
import {
  bookingToForm,
  calculateBalance,
  hasAvailabilityConflict,
  toBookingPayload,
} from '../bookingLogic.js';
import {
  deleteBooking as deleteBookingRequest,
  logout as logoutRequest,
  fetchBookingReport,
  fetchBookings,
  fetchResources,
  saveBooking,
  updateBooking,
} from '../bookingApi.js';
import { BookingForm } from './BookingForm.jsx';
import { BookingTimeline } from './BookingTimeline.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';
import { useTheme } from '../useTheme.js';
import { reportDocument } from '../reportDocument.js';
import { formatMoney, useLanguage } from '../i18n.jsx';
import { LanguageSelector } from './LanguageSelector.jsx';

function todayAsIsoDate() {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
}

function emptyForm() {
  const today = todayAsIsoDate();
  return {
    name: '',
    numOfGuests: '',
    agency: '',
    room: '',
    country: '',
    startDate: today,
    endDate: today,
    charge: '',
    commission: '',
    received: '',
    status: 'active',
    identification: '',
    afm: '',
    email: '',
    tel: '',
    voucher: '',
    taxRefNumber: '',
    invoiceNumber: '',
    extraInfo: '',
  };
}

function getMode() {
  const requestedMode = new URLSearchParams(window.location.search).get('type');
  return bookingModes[requestedMode] ?? bookingModes.rooms;
}

function formatClock(date, locale) {
  return date.toLocaleTimeString(locale, { hour: 'numeric', minute: '2-digit', second: '2-digit' });
}

export function BookingPage() {
  const baseMode = useMemo(getMode, []);
  const bookableTimelineRef = useRef(null);
  const cancelledTimelineRef = useRef(null);
  const [form, setForm] = useState(emptyForm);
  const [bookings, setBookings] = useState([]);
  const [resources, setResources] = useState([]);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saveMode, setSaveMode] = useState(getInitialBookingSaveMode);
  const [reportHtml, setReportHtml] = useState('');
  const { theme, toggleTheme } = useTheme();
  const { language, locale, t } = useLanguage();
  const [clock, setClock] = useState(() => formatClock(new Date(), locale));

  useEffect(() => {
    let active = true;
    Promise.all([fetchBookings(), fetchResources()])
      .then(([loadedBookings, loadedResources]) => {
        if (active) {
          setBookings(loadedBookings);
          setResources(loadedResources);
        }
      })
      .catch((requestError) => {
        if (active) setError(t('Could not load bookings: {error}', { error: requestError.message }));
      });

    return () => {
      active = false;
    };
  }, [t]);

  useEffect(() => {
    document.documentElement.lang = language;
    const timerId = window.setInterval(() => setClock(formatClock(new Date(), locale)), 1000);
    return () => window.clearInterval(timerId);
  }, [language, locale]);

  function resetForm() {
    setForm(emptyForm());
    setSelectedBooking(null);
    setError('');
    setNotice('');
    bookableTimelineRef.current?.clearSelection();
    cancelledTimelineRef.current?.clearSelection();
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setNotice('');

    const payload = toBookingPayload(form);
    if (!payload.name || !payload.room || !payload.agency || !payload.numOfGuests) {
      setError(t('Complete the required name, guest, agency, and resource fields.'));
      return;
    }
    if (!payload.startDate || !payload.endDate || payload.numOfNights <= 0) {
      setError(t('The end date must be after the start date.'));
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError(t('Enter a valid email address.'));
      return;
    }
    const ignoredBookingId = saveMode === 'edit' ? selectedBooking?.id : null;
    if (hasAvailabilityConflict(payload, bookings, ignoredBookingId)) {
      setError(t(mode.unavailableMessage));
      return;
    }

    setBusy(true);
    try {
      const isEditing = saveMode === 'edit' && selectedBooking;
      const savedBooking = isEditing
        ? await updateBooking({ ...payload, id: selectedBooking.id })
        : await saveBooking(payload);
      setBookings((current) => (
        isEditing
          ? current.map((booking) => booking.id === savedBooking.id ? savedBooking : booking)
          : [...current, savedBooking]
      ));
      if (isEditing) setSelectedBooking(savedBooking);
      if (isBookableStatus(savedBooking.status)) {
        setNotice(t(isEditing ? 'Booking updated.' : 'Booking saved.'));
      } else {
        setNotice(isEditing
          ? t('Cancelled booking updated.')
          : t('Cancelled booking saved and shown in the cancelled bookings timeline.'));
      }
    } catch (requestError) {
      setError(t('Could not save booking: {error}', { error: requestError.message }));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(id) {
    try {
      await deleteBookingRequest(id);
      setBookings((current) => current.filter((booking) => booking.id !== id));
      setSelectedBooking((current) => current?.id === id ? null : current);
      setNotice(t('Booking deleted.'));
    } catch (requestError) {
      setError(t('Could not delete booking: {error}', { error: requestError.message }));
      throw requestError;
    }
  }

  async function handleReport() {
    setError('');
    setNotice('');
    if (!form.startDate || !form.endDate || form.startDate > form.endDate) {
      setError(t('Choose a valid date range for the report.'));
      return;
    }
    try {
      setReportHtml(await fetchBookingReport(form.startDate, form.endDate, language));
    } catch (requestError) {
      setError(t('Could not load report: {error}', { error: requestError.message }));
    }
  }

  function handleSelect(booking) {
    setSelectedBooking(booking);
    setForm(bookingToForm(booking));
    setError('');
    setNotice('');
  }

  function handleDateNavigation(action) {
    if (action === 'today') {
      bookableTimelineRef.current?.moveToToday();
      cancelledTimelineRef.current?.moveToToday();
      return;
    }
    if (action === 'goto') {
      const [year, month, day] = form.startDate.split('-').map(Number);
      const date = new Date(year, month - 1, day, 12);
      bookableTimelineRef.current?.moveToDate(date);
      cancelledTimelineRef.current?.moveToDate(date);
      return;
    }
    bookableTimelineRef.current?.shiftDays(action);
    cancelledTimelineRef.current?.shiftDays(action);
  }

  function handleSaveModeChange(event) {
    const nextMode = event.target.checked ? 'edit' : 'create';
    setSaveMode(storeBookingSaveMode(nextMode));
    setNotice('');
  }

  async function handleLogout() {
    setError('');
    try {
      await logoutRequest();
    } catch (requestError) {
      setError(t('Could not sign out: {error}', { error: requestError.message }));
    }
  }

  const balance = calculateBalance(form.charge, form.received, form.commission);
  const mode = useMemo(() => ({
    ...baseMode,
    resources: resourcesForMode(resources, baseMode),
  }), [baseMode, resources]);

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">{t('Karpathos booking planner')}</p>
          <h1>{t(mode.title)}</h1>
        </div>
        <div className="header-meta">
          <nav aria-label={t('Booking type')}>
            <a href="/?type=rooms" aria-current={mode.id === 'rooms' ? 'page' : undefined}>{t('Rooms')}</a>
            <a href="/?type=cars" aria-current={mode.id === 'cars' ? 'page' : undefined}>{t('Cars & bikes')}</a>
            <a className="nav-icon-link" href={`/?type=${mode.id}&page=reports`}>
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
            <a href="/?page=resources">{t('Manage rooms and cars')}</a>
          </nav>
          <LanguageSelector />
          <ThemeToggle theme={theme} onToggle={toggleTheme} />
          <label className="save-mode-toggle">
            <input
              type="checkbox"
              checked={saveMode === 'edit'}
              onChange={handleSaveModeChange}
            />
            <span>{t('Edit selected booking')} <small>{t('(testing)')}</small></span>
          </label>
          <button type="button" className="button button-muted" onClick={handleLogout}>{t('Sign out')}</button>
          <time>{clock}</time>
        </div>
      </header>

      <section className="panel" aria-labelledby="booking-form-heading">
        <div className="panel-heading">
          <div>
            <h2 id="booking-form-heading">{t('Booking details')}</h2>
            <p>{t('Enter a new booking or select one on the timeline to review it.')}</p>
          </div>
          <strong className="balance-summary">{t('Balance: {amount}', { amount: formatMoney(balance, locale) })}</strong>
        </div>

        {error && <p className="feedback feedback-error" role="alert">{error}</p>}
        {notice && <p className="feedback feedback-success" role="status">{notice}</p>}

        <BookingForm
          mode={mode}
          form={form}
          onChange={setForm}
          onSubmit={handleSubmit}
          onReset={resetForm}
          selectedBooking={selectedBooking}
          saveMode={saveMode}
          busy={busy}
        />

        <div className="toolbar" aria-label={t('Timeline controls')}>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('goto')}>{t('Go to start date')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('today')}>{t('Today')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-90)}>{t('90 days back')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-30)}>{t('30 days back')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(30)}>{t('30 days forward')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(90)}>{t('90 days forward')}</button>
          <button type="button" className="button button-primary" onClick={handleReport}>{t('Run report')}</button>
        </div>
      </section>

      <BookingTimeline
        ref={bookableTimelineRef}
        mode={mode}
        bookings={bookings}
        timelineType="bookable"
        title={t(mode.id === 'rooms' ? 'Rooms availability' : 'Vehicle availability')}
        description={t('Select a booking to load its details. Drag the timeline to move through dates.')}
        onSelect={handleSelect}
        onDelete={handleDelete}
        onError={setError}
      />

      <BookingTimeline
        ref={cancelledTimelineRef}
        mode={mode}
        bookings={bookings}
        timelineType="cancelled"
        title={t('Cancelled bookings')}
        description={t('Cancelled reservations are listed separately and do not block availability.')}
        onSelect={handleSelect}
        onDelete={handleDelete}
        onError={setError}
      />

      {reportHtml && (
        <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setReportHtml('');
        }}>
          <section className="report-dialog" role="dialog" aria-modal="true" aria-labelledby="report-heading">
            <div className="report-heading">
              <h2 id="report-heading">{t('Booking report')}</h2>
              <button type="button" className="button button-muted" onClick={() => setReportHtml('')}>{t('Close')}</button>
            </div>
            <iframe title={t('Booking report')} sandbox="allow-same-origin" srcDoc={reportDocument(reportHtml, theme, language)} />
          </section>
        </div>
      )}
    </main>
  );
}
