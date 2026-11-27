import { useState } from 'react';
import { guestCounts } from '../bookingConfig.js';
import { parseBookingEmail } from '../bookingEmailImport.js';
import countries from '../countries.json';
import {
  estimateBalance,
  estimateRentalSubTotal,
  estimateRentalTax,
  nightsBetween,
} from '../bookingLogic.js';
import { formatMoney, useLanguage } from '../i18n.jsx';

const detailFields = [
  { name: 'identification', label: 'Passport / ID' },
  { name: 'afm', label: 'Tax number (ΑΦΜ)' },
  { name: 'email', label: 'Email', type: 'email', autoComplete: 'email' },
  { name: 'tel', label: 'Telephone', type: 'tel', autoComplete: 'tel' },
  { name: 'voucher', label: 'Voucher' },
  { name: 'taxRefNumber', label: 'Tax reference' },
  { name: 'invoiceNumber', label: 'Invoice number' },
  { name: 'platformReservationId', label: 'Platform reservation ID' },
];

export function BookingForm({
  mode,
  form,
  onChange,
  onSubmit,
  onReset,
  selectedBooking,
  saveMode,
  onSaveModeChange,
  busy,
  canManage = true,
  bookings = [],
}) {
  const [showDetails, setShowDetails] = useState(false);
  const [showFinancials, setShowFinancials] = useState(true);
  const [emailText, setEmailText] = useState('');
  const [emailLanguage, setEmailLanguage] = useState('auto');
  const [importWarnings, setImportWarnings] = useState([]);
  const [importNotes, setImportNotes] = useState([]);
  const [importedFields, setImportedFields] = useState([]);
  const nights = nightsBetween(form.startDate, form.endDate);
  const { locale, t } = useLanguage();
  const readOnly = !canManage;
  const selectedResource = mode.resources.find((resource) => resource.id === form.resourceId);
  const importWarningLabels = {
    NOTHING_FOUND: t('No booking details found in the pasted text.'),
    RESERVATION_ID_MISSING: t('Reservation number not found.'),
    GUEST_NAME_MISSING: t('Guest name not found.'),
    DATES_MISSING: t('Dates not found.'),
    GUEST_COUNT_MISSING: t('Guest count not found.'),
    RESOURCE_NOT_MATCHED: t('Resource not matched — select it manually.'),
    PROPERTY_PHONE_IGNORED: t('Property phone ignored (guest telephone left blank).'),
    PROPERTY_ADDRESS_IGNORED: t('Property address ignored (guest country left blank).'),
    PIN_IGNORED: t('PIN code ignored.'),
    MONEY_NOT_FILLED: t('Money found but not saved to the financial fields.'),
    MULTIPLE_GUESTS: t('Multiple guests joined in the name field.'),
    DUPLICATE_RESERVATION_ID: t('This reservation ID already exists on another booking.'),
  };
  const rentalTaxEstimate = estimateRentalTax(form.startDate, form.endDate, selectedResource);

  function fieldProps(name, base = 'field') {
    return {
      className: importedFields.includes(name) ? `${base} field-imported` : base,
    };
  }

  function clearImportedFlag(name) {
    setImportedFields((current) => current.filter((field) => field !== name));
  }

  function update(name, value) {
    clearImportedFlag(name);
    onChange({ ...form, [name]: value });
  }

  function updateDate(name, value) {
    clearImportedFlag(name);
    if (name === 'startDate') clearImportedFlag('endDate');
    if (name === 'startDate') {
      onChange({ ...form, startDate: value, endDate: value });
      return;
    }
    onChange({ ...form, endDate: value });
  }

  function updateMoney(name, value) {
    clearImportedFlag(name);
    onChange({ ...form, [name]: value });
  }

  function applyEmailImport() {
    const result = parseBookingEmail(emailText, emailLanguage);
    const next = { ...form };
    for (const [key, value] of Object.entries(result.values)) {
      if (value !== undefined && value !== null && String(value) !== '') next[key] = value;
    }

    const warnings = [...result.warnings];
    const extractedNames = [result.propertyName, result.unitName]
      .filter(Boolean)
      .map((name) => name.trim().toLowerCase());
    const matchedResource = mode.resources.find((resource) => (
      extractedNames.includes(String(resource.text ?? resource.name ?? '').trim().toLowerCase())
    ));
    if (matchedResource) next.resourceId = matchedResource.id;
    else if (extractedNames.length) warnings.push('RESOURCE_NOT_MATCHED');

    const reservationId = result.values.platformReservationId;
    const duplicate = reservationId && bookings.some((booking) => (
      booking.id !== selectedBooking?.id && booking.platformReservationId === reservationId
    ));
    if (duplicate) warnings.push('DUPLICATE_RESERVATION_ID');

    let addedNotes = [];
    if (result.notes.length) {
      const currentNotes = next.extraInfo?.trim() || '';
      addedNotes = result.notes
        .map((note) => t('From email — {label}: {value} (not saved to financial fields)', {
          label: note.label,
          value: note.value,
        }))
        .filter((line) => !currentNotes.includes(line));
      if (addedNotes.length) next.extraInfo = [currentNotes, ...addedNotes].filter(Boolean).join('\n');
    }

    const imported = [
      ...Object.keys(result.values),
      ...(matchedResource ? ['resourceId'] : []),
      ...(addedNotes.length ? ['extraInfo'] : []),
    ];
    onChange(next);
    setImportWarnings(warnings);
    setImportNotes(result.notes);
    setImportedFields([...new Set(imported)]);
    setEmailText('');
  }

  const balanceValue = estimateBalance(form.charge, form.received, form.commission, rentalTaxEstimate);
  const rentalSubTotalValue = estimateRentalSubTotal(form.charge, rentalTaxEstimate);

  return (
    <form className="booking-form" onSubmit={onSubmit}>
      {canManage && (
        <section className="form-section" aria-labelledby="email-import-heading">
          <div className="panel-heading">
            <div>
              <h3 id="email-import-heading">{t('Import from Booking.com email')}</h3>
              <p>{t('Paste the confirmation email text. Values are filled into the form for review; nothing is saved.')}</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="field field-notes">
              <span>{t('Email text')}</span>
              <textarea rows="4" value={emailText} onChange={(event) => setEmailText(event.target.value)} />
            </label>
            <label className="field">
              <span>{t('Language')}</span>
              <select value={emailLanguage} onChange={(event) => setEmailLanguage(event.target.value)}>
                <option value="auto">{t('Auto')}</option>
                <option value="en">{t('English')}</option>
                <option value="el">{t('Greek')}</option>
              </select>
            </label>
          </div>
          <div className="form-actions">
            <button
              type="button"
              className="button button-muted"
              onClick={applyEmailImport}
              disabled={!emailText.trim()}
            >
              {t('Fill form')}
            </button>
          </div>
          {importedFields.length > 0 && (
            <p className="form-note">{t('Highlighted fields were filled from the email. Review them before saving.')}</p>
          )}
          {importWarnings.length > 0 && (
            <ul className="form-note">
              {importWarnings.map((code, index) => (
                <li key={`${code}-${index}`}>{importWarningLabels[code] ?? code}</li>
              ))}
            </ul>
          )}
          {importNotes.length > 0 && (
            <ul className="form-note">
              {importNotes.map((note, index) => (
                <li key={`note-${index}`}>
                  {t('From email — {label}: {value} (not saved to financial fields)', { label: note.label, value: note.value })}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <fieldset disabled={readOnly} className="reset-fieldset">
        <section className="form-section" aria-labelledby="stay-section-heading">
          <div className="panel-heading">
            <div>
              <h3 id="stay-section-heading">{t('Stay / Reservation')}</h3>
              <p>{t('Set dates, resource, status, and guests first.')}</p>
            </div>
          </div>
          <div className="form-grid">
            <label {...fieldProps('startDate')}>
              <span>{t('From *')}</span>
              <input required type="date" value={form.startDate} onChange={(event) => updateDate('startDate', event.target.value)} />
            </label>

            <label {...fieldProps('endDate')}>
              <span>{t('To *')}</span>
              <input required type="date" min={form.startDate || undefined} value={form.endDate} onChange={(event) => updateDate('endDate', event.target.value)} />
            </label>

            <label className="field">
              <span>{t('Nights')}</span>
              <input readOnly tabIndex={-1} value={nights} />
            </label>

            <label {...fieldProps('resourceId')}>
              <span>{t(mode.resourceLabel)} *</span>
              <select required value={form.resourceId} onChange={(event) => update('resourceId', event.target.value)}>
                <option value="">{t('Select')}</option>
                {mode.resources.filter(({ id, active = true }) => active || id === form.resourceId).map(({ id, text, active = true }) => (
                  <option value={id} key={id}>{text}{!active ? ` (${t('Inactive')})` : ''}</option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>{t('Status *')}</span>
              <select required value={form.status} onChange={(event) => update('status', event.target.value)}>
                {mode.statuses.map(({ id, text }) => (
                  <option value={id} key={id}>{t(text)}</option>
                ))}
              </select>
            </label>

            <label {...fieldProps('numOfGuests')}>
              <span>{t('Guests *')}</span>
              <select required value={form.numOfGuests} onChange={(event) => update('numOfGuests', event.target.value)}>
                <option value="">{t('Select')}</option>
                {guestCounts.map((count) => (
                  <option value={count} key={count}>{count}</option>
                ))}
              </select>
            </label>
            {canManage && onSaveModeChange ? (
              <div className="field">
                <span>{t('Save mode')}</span>
                <button
                  type="button"
                  className="button button-muted save-mode-toggle"
                  aria-pressed={saveMode === 'edit'}
                  onClick={onSaveModeChange}
                >
                  <span aria-hidden="true">{saveMode === 'edit' ? '✏️' : '📝'}</span>
                  <span>{saveMode === 'edit' ? t('Update mode') : t('Remove & add mode')}</span>
                </button>
              </div>
            ) : null}
          </div>
        </section>

        <section className="form-section" aria-labelledby="guest-section-heading">
          <div className="panel-heading">
            <div>
              <h3 id="guest-section-heading">{t('Guest & Channel')}</h3>
            </div>
          </div>
          <div className="form-grid">
            <label {...fieldProps('name', 'field field-name')}>
              <span>{t('Name *')}</span>
              <input required autoComplete="name" value={form.name} onChange={(event) => update('name', event.target.value)} />
            </label>

            <label {...fieldProps('agency')}>
              <span>{t('Agency / color *')}</span>
              <select required value={form.agency} onChange={(event) => update('agency', event.target.value)}>
                <option value="">{t('Select')}</option>
                {mode.agencies.map(({ id, text }) => (
                  <option value={id} key={id}>{t(text)}</option>
                ))}
              </select>
            </label>

            <label {...fieldProps('country')}>
              <span>{t('Country')}</span>
              <input list="country-options" value={form.country} onChange={(event) => update('country', event.target.value)} />
              <datalist id="country-options">
                {countries.map((country) => (
                  <option value={country} key={country}>{country}</option>
                ))}
              </datalist>
            </label>
          </div>
        </section>

      </fieldset>

      <section className="form-section" aria-labelledby="financial-section-heading">
        <div className="panel-heading">
          <div>
            <h3 id="financial-section-heading">{t('Financials')}</h3>
            <p>{t('Balance = Total charged − Received − Commission − Rental tax')}</p>
            <p>{t('Rental subtotal = Total charged − Rental tax')}</p>
            <p className="form-note">{t('Estimates only; the saved values are calculated by the server.')}</p>
          </div>
          <button type="button" className="button button-muted" onClick={() => setShowFinancials((v) => !v)}>
            {t(showFinancials ? 'Hide financials' : 'Show financials')}
          </button>
        </div>
        {!showFinancials && (
          <p className="form-note">
            {t('Balance (€)')}: {formatMoney(balanceValue, locale)} · {t('Rental tax (€)')}: {formatMoney(rentalTaxEstimate, locale)} · {t('Rental subtotal (€)')}: {formatMoney(rentalSubTotalValue, locale)}
          </p>
        )}
        {showFinancials && (
          <fieldset disabled={readOnly} className="reset-fieldset">
            <div className="form-grid">
              <label className="field">
                <span>{t('Total charged (€)')}</span>
                <input type="number" min="0" step="0.01" value={form.charge} onChange={(event) => updateMoney('charge', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Commission (€)')}</span>
                <input type="number" min="0" step="0.01" value={form.commission} onChange={(event) => updateMoney('commission', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Received (€)')}</span>
                <input type="number" min="0" step="0.01" value={form.received} onChange={(event) => updateMoney('received', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Rental tax per night (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(Number(selectedResource?.rentalTaxPerNight) || 0, locale)} />
              </label>

              <label className="field">
                <span>{t('Rental tax (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(rentalTaxEstimate, locale)} />
              </label>

              <label className="field">
                <span>{t('Rental subtotal (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(rentalSubTotalValue, locale)} />
              </label>

              <label className="field">
                <span>{t('Balance (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(balanceValue, locale)} />
              </label>
            </div>
          </fieldset>
        )}
      </section>

      <div className="form-details-toggle">
        <button type="button" className="button button-muted" onClick={() => setShowDetails(!showDetails)}>
          {t(showDetails ? 'Hide details' : 'Show details')}
        </button>
      </div>

      <fieldset disabled={readOnly} className="reset-fieldset">
        {showDetails && (
          <div className="details-grid">
            {detailFields.map(({ name, label, type = 'text', autoComplete }) => (
              <label {...fieldProps(name)} key={name}>
                <span>{t(label)}</span>
                <input type={type} autoComplete={autoComplete} value={form[name]} onChange={(event) => update(name, event.target.value)} />
              </label>
            ))}
            <label {...fieldProps('extraInfo', 'field field-notes')}>
              <span>{t('Notes')}</span>
              <textarea rows="3" value={form.extraInfo} onChange={(event) => update('extraInfo', event.target.value)} />
            </label>
          </div>
        )}
      </fieldset>

      {selectedBooking && canManage && (
        <p className="form-note">
          {t('Selected booking {name} ({id}).', { name: selectedBooking.name, id: selectedBooking.id })}{' '}
          {saveMode === 'edit'
            ? t('Saving updates this booking.')
            : t('Saving creates a new booking and leaves this one unchanged.')}
        </p>
      )}

      {canManage && (
        <div className="form-actions">
          <button className="button button-primary" type="submit" disabled={busy || nights <= 0}>
            {busy
              ? (saveMode === 'edit' && selectedBooking ? t('Updating…') : t('Saving…'))
              : (saveMode === 'edit' && selectedBooking ? t('Update booking') : t('Save as new booking'))}
          </button>
          <button className="button button-muted" type="button" onClick={onReset} disabled={busy}>
            {t('New / reset')}
          </button>
        </div>
      )}
    </form>
  );
}
