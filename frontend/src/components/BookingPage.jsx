import { useEffect, useMemo, useRef, useState } from 'react';
import { bookingModes, isBookableStatus } from '../bookingConfig.js';
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
  fetchBookingReport,
  fetchBookings,
  saveBooking,
  updateBooking,
} from '../bookingApi.js';
import { BookingForm } from './BookingForm.jsx';
import { BookingTimeline } from './BookingTimeline.jsx';

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

function formatClock(date) {
  return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', second: '2-digit' });
}

export function BookingPage() {
  const mode = useMemo(getMode, []);
  const bookableTimelineRef = useRef(null);
  const cancelledTimelineRef = useRef(null);
  const [form, setForm] = useState(emptyForm);
  const [bookings, setBookings] = useState([]);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saveMode, setSaveMode] = useState(getInitialBookingSaveMode);
  const [reportHtml, setReportHtml] = useState('');
  const [clock, setClock] = useState(() => formatClock(new Date()));

  useEffect(() => {
    let active = true;
    fetchBookings()
      .then((loadedBookings) => {
        if (active) setBookings(loadedBookings);
      })
      .catch((requestError) => {
        if (active) setError(`Could not load bookings: ${requestError.message}`);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const timerId = window.setInterval(() => setClock(formatClock(new Date())), 1000);
    return () => window.clearInterval(timerId);
  }, []);

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
      setError('Complete the required name, guest, agency, and resource fields.');
      return;
    }
    if (!payload.startDate || !payload.endDate || payload.numOfNights <= 0) {
      setError('The end date must be after the start date.');
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError('Enter a valid email address.');
      return;
    }
    const ignoredBookingId = saveMode === 'edit' ? selectedBooking?.id : null;
    if (hasAvailabilityConflict(payload, bookings, ignoredBookingId)) {
      setError(mode.unavailableMessage);
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
        setNotice(isEditing ? 'Booking updated.' : 'Booking saved.');
      } else {
        setNotice(isEditing
          ? 'Cancelled booking updated.'
          : 'Cancelled booking saved and shown in the cancelled bookings timeline.');
      }
    } catch (requestError) {
      setError(`Could not save booking: ${requestError.message}`);
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(id) {
    await deleteBookingRequest(id);
    setBookings((current) => current.filter((booking) => booking.id !== id));
    setSelectedBooking((current) => current?.id === id ? null : current);
    setNotice('Booking deleted.');
  }

  async function handleReport() {
    setError('');
    setNotice('');
    if (!form.startDate || !form.endDate || form.startDate > form.endDate) {
      setError('Choose a valid date range for the report.');
      return;
    }
    try {
      setReportHtml(await fetchBookingReport(form.startDate, form.endDate));
    } catch (requestError) {
      setError(`Could not load report: ${requestError.message}`);
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

  const balance = calculateBalance(form.charge, form.received, form.commission);

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Karpathos booking planner</p>
          <h1>{mode.title}</h1>
        </div>
        <div className="header-meta">
          <nav aria-label="Booking type">
            <a href="/?type=rooms" aria-current={mode.id === 'rooms' ? 'page' : undefined}>Rooms</a>
            <a href="/?type=cars" aria-current={mode.id === 'cars' ? 'page' : undefined}>Cars &amp; bikes</a>
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
              <span>Reports</span>
            </a>
          </nav>
          <label className="save-mode-toggle">
            <input
              type="checkbox"
              checked={saveMode === 'edit'}
              onChange={handleSaveModeChange}
            />
            <span>Edit selected booking <small>(testing)</small></span>
          </label>
          <time>{clock}</time>
        </div>
      </header>

      <section className="panel" aria-labelledby="booking-form-heading">
        <div className="panel-heading">
          <div>
            <h2 id="booking-form-heading">Booking details</h2>
            <p>Enter a new booking or select one on the timeline to review it.</p>
          </div>
          <strong className="balance-summary">Balance: €{balance.toFixed(2)}</strong>
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

        <div className="toolbar" aria-label="Timeline controls">
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('goto')}>Go to start date</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('today')}>Today</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-90)}>« 90 days</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-30)}>‹ 30 days</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(30)}>30 days ›</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(90)}>90 days »</button>
          <button type="button" className="button button-primary" onClick={handleReport}>Run report</button>
        </div>
      </section>

      <BookingTimeline
        ref={bookableTimelineRef}
        mode={mode}
        bookings={bookings}
        timelineType="bookable"
        title={`${mode.resourceLabel} availability`}
        description="Select a booking to load its details. Drag the timeline to move through dates."
        onSelect={handleSelect}
        onDelete={handleDelete}
        onError={setError}
      />

      <BookingTimeline
        ref={cancelledTimelineRef}
        mode={mode}
        bookings={bookings}
        timelineType="cancelled"
        title="Cancelled bookings"
        description="Cancelled reservations are listed separately and do not block availability."
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
              <h2 id="report-heading">Booking report</h2>
              <button type="button" className="button button-muted" onClick={() => setReportHtml('')}>Close</button>
            </div>
            <iframe title="Booking report" sandbox="allow-same-origin" srcDoc={reportHtml} />
          </section>
        </div>
      )}
    </main>
  );
}
