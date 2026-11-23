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
  fetchAvailabilitySettings,
  fetchBookings,
  fetchResources,
  isAuthRedirectError,
  saveBooking,
  updateBooking,
} from '../bookingApi.js';
import { fetchMe } from '../usersApi.js';
import { BookingForm } from './BookingForm.jsx';
import { BookingTimeline } from './BookingTimeline.jsx';
import { FeedbackDialog } from './FeedbackDialog.jsx';
import { AppHeader } from './AppHeader.jsx';
import { useTheme } from '../useTheme.js';
import { formatMoney, useLanguage } from '../i18n.jsx';
import { useCurrentUser } from '../useCurrentUser.js';

function todayAsIsoDate() {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
}

function formatHeadingDate(value, locale) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '');
  if (!match) return value ?? '';
  const [, year, month, day] = match;
  return new Intl.DateTimeFormat(locale).format(
    new Date(Number(year), Number(month) - 1, Number(day)),
  );
}

function emptyForm() {
  const today = todayAsIsoDate();
  return {
    name: '',
    numOfGuests: '',
    agency: '',
    resourceId: '',
    country: '',
    startDate: today,
    endDate: today,
    charge: '',
    commission: '',
    received: '',
    rentalTax: '',
    status: 'active',
    identification: '',
    afm: '',
    email: '',
    tel: '',
    voucher: '',
    taxRefNumber: '',
    invoiceNumber: '',
    platformReservationId: '',
    extraInfo: '',
  };
}

function getMode() {
  const requestedMode = new URLSearchParams(window.location.search).get('type');
  return bookingModes[requestedMode] ?? bookingModes.rooms;
}



export function BookingPage() {
  const baseMode = useMemo(getMode, []);
  const bookableTimelineRef = useRef(null);
  const cancelledTimelineRef = useRef(null);
  const [form, setForm] = useState(emptyForm);
  const [bookings, setBookings] = useState([]);
  const [me, setMe] = useState(null);
  const [resources, setResources] = useState([]);
  const [availabilitySettings, setAvailabilitySettings] = useState({
    roomsPreventOverlap: true,
    carsPreventOverlap: true,
  });
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errorPopupOpen, setErrorPopupOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const [saveMode, setSaveMode] = useState(getInitialBookingSaveMode);
  const [formVisible, setFormVisible] = useState(true);
  const { theme, toggleTheme } = useTheme();
  const { language, locale, t } = useLanguage();
  const { currentUser } = useCurrentUser();
  const isAdmin = currentUser?.role === 'ADMIN';
  const isOperator = currentUser?.role === 'OPERATOR';
  const canManageBookings = isAdmin || isOperator;
  const canSeeReports = isAdmin || isOperator;


  function showError(message) {
    setError(message);
    setErrorPopupOpen(Boolean(message));
  }

  useEffect(() => {
    let active = true;
    Promise.all([fetchBookings(), fetchResources(), fetchAvailabilitySettings()])
      .then(([loadedBookings, loadedResources, loadedAvailabilitySettings]) => {
        if (active) {
          setBookings(loadedBookings);
          setResources(loadedResources);
          setAvailabilitySettings(loadedAvailabilitySettings);
        }
      })
      .catch((requestError) => {
        if (active && !isAuthRedirectError(requestError)) {
          showError(t('Could not load bookings: {error}', { error: requestError.message }));
        }
      });

    return () => {
      active = false;
    };
  }, [t]);

  useEffect(() => {
    let active = true;
    fetchMe()
      .then((user) => {
        if (active) setMe(user);
      })
      .catch(() => {
        if (active) setMe(null);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  function resetForm() {
    setForm(emptyForm());
    setSelectedBooking(null);
    showError('');
    setNotice('');
    bookableTimelineRef.current?.clearSelection();
    cancelledTimelineRef.current?.clearSelection();
  }

  async function handleSubmit(event) {
    event.preventDefault();
    showError('');
    setNotice('');

    const payload = toBookingPayload(form);
    if (!payload.name || !payload.resourceId || !payload.agency || !payload.numOfGuests) {
      showError(t('Complete the required name, guest, agency, and resource fields.'));
      return;
    }
    if (!payload.startDate || !payload.endDate || payload.numOfNights <= 0) {
      showError(t('The end date must be after the start date.'));
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      showError(t('Enter a valid email address.'));
      return;
    }
    const ignoredBookingId = saveMode === 'edit' ? selectedBooking?.id : null;
    const preventOverlap = baseMode.id === 'rooms'
      ? availabilitySettings.roomsPreventOverlap
      : availabilitySettings.carsPreventOverlap;
    if (preventOverlap && hasAvailabilityConflict(payload, bookings, ignoredBookingId)) {
      showError(t(mode.unavailableMessage));
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
      showError(t('Could not save booking: {error}', { error: requestError.message }));
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
      showError(t('Could not delete booking: {error}', { error: requestError.message }));
      throw requestError;
    }
  }

  function handleSelect(booking) {
    setSelectedBooking(booking);
    setForm(bookingToForm(booking));
    showError('');
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

  function handleSaveModeChange() {
    const nextMode = saveMode === 'edit' ? 'create' : 'edit';
    setSaveMode(storeBookingSaveMode(nextMode));
    setNotice('');
  }

  async function handleLogout() {
    showError('');
    try {
      await logoutRequest();
    } catch (requestError) {
      showError(t('Could not sign out: {error}', { error: requestError.message }));
    }
  }

  const balance = calculateBalance(form.charge, form.received, form.commission, form.rentalTax);
  const mode = useMemo(() => ({
    ...baseMode,
    resources: resourcesForMode(resources, baseMode),
  }), [baseMode, resources]);

  return (
    <main className="app-shell">
      <AppHeader
        title={t(mode.title)}
        currentPage={mode.id}
        modeId={mode.id}
        isAdmin={isAdmin}
        canAccessReports={canSeeReports}
        signedInLabel={me ? t('Signed in as {username}', { username: `${me.username} (${me.role})` }) : ''}
        theme={theme}
        onToggleTheme={toggleTheme}
        onLogout={handleLogout}
      />

      <section className="panel" aria-labelledby="booking-form-heading">
        <div className="panel-heading">
          <div>
            <h2 id="booking-form-heading">{t('Booking details')}</h2>
            <p>{t('Enter a new booking or select one on the timeline to review it.')}</p>
            <p className="booking-details-summary" aria-live="polite">
              {form.name.trim() && <span><strong>{t('Name')}:</strong> {form.name.trim()}</span>}
              {form.startDate && <span><strong>{t('From')}:</strong> {formatHeadingDate(form.startDate, locale)}</span>}
              {form.endDate && <span><strong>{t('To')}:</strong> {formatHeadingDate(form.endDate, locale)}</span>}
              {form.tel.trim() && <span><strong>{t('Telephone')}:</strong> {form.tel.trim()}</span>}
            </p>
          </div>
          <div className="booking-form-heading-actions">
            <strong className="balance-summary">{t('Balance: {amount}', { amount: formatMoney(balance, locale) })}</strong>
            <button
              type="button"
              className="button button-muted"
              aria-expanded={formVisible}
              aria-controls="booking-form"
              onClick={() => setFormVisible((visible) => !visible)}
            >
              {t(formVisible ? 'Hide form' : 'Show form')}
            </button>
          </div>
        </div>

        {error && <p className="feedback feedback-error" role="alert">{error}</p>}
        {notice && <p className="feedback feedback-success" role="status">{notice}</p>}

        <div id="booking-form" hidden={!formVisible}>
          <BookingForm
            mode={mode}
            form={form}
            onChange={setForm}
            onSubmit={handleSubmit}
            onReset={resetForm}
            selectedBooking={selectedBooking}
            saveMode={saveMode}
            onSaveModeChange={handleSaveModeChange}
            busy={busy}
            canManage={canManageBookings}
          />
        </div>

        <div className="toolbar" aria-label={t('Timeline controls')}>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('goto')}>{t('Go to start date')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation('today')}>{t('Today')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-90)}>{t('90 days back')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(-30)}>{t('30 days back')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(30)}>{t('30 days forward')}</button>
          <button type="button" className="button button-muted" onClick={() => handleDateNavigation(90)}>{t('90 days forward')}</button>
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
        onError={showError}
        canDelete={canManageBookings}
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
        onError={showError}
        canDelete={canManageBookings}
      />

      <FeedbackDialog
        message={errorPopupOpen ? error : ''}
        onClose={() => setErrorPopupOpen(false)}
      />
    </main>
  );
}
