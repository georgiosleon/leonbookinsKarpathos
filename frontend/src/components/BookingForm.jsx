import { useState } from 'react';
import { guestCounts } from '../bookingConfig.js';
import countries from '../countries.json';
import { nightsBetween } from '../bookingLogic.js';
import { formatMoney, useLanguage } from '../i18n.jsx';

const detailFields = [
  { name: 'identification', label: 'Passport / ID' },
  { name: 'afm', label: 'Tax number (ΑΦΜ)' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'tel', label: 'Telephone' },
  { name: 'voucher', label: 'Voucher' },
  { name: 'taxRefNumber', label: 'Tax reference' },
  { name: 'invoiceNumber', label: 'Invoice number' },
];

export function BookingForm({
  mode,
  form,
  onChange,
  onSubmit,
  onReset,
  selectedBooking,
  saveMode,
  busy,
  canManage = true,
}) {
  const [showDetails, setShowDetails] = useState(true);
  const nights = nightsBetween(form.startDate, form.endDate);
  const { locale, t } = useLanguage();
  const readOnly = !canManage;

  function update(name, value) {
    onChange({ ...form, [name]: value });
  }

  function updateDate(name, value) {
    if (name === 'startDate') {
      onChange({ ...form, startDate: value, endDate: value });
      return;
    }
    onChange({ ...form, endDate: value });
  }

  function updateMoney(name, value) {
    onChange({ ...form, [name]: value });
  }

  return (
    <form className="booking-form" onSubmit={onSubmit}>
      <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minInlineSize: 0 }}>
      <div className="form-grid">
        <label className="field field-name">
          <span>{t('Name *')}</span>
          <input
            required
            autoComplete="name"
            value={form.name}
            onChange={(event) => update('name', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('Guests *')}</span>
          <select
            required
            value={form.numOfGuests}
            onChange={(event) => update('numOfGuests', event.target.value)}
          >
            <option value="">{t('Select')}</option>
            {guestCounts.map((count) => (
              <option value={count} key={count}>{count}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>{t('Agency / color *')}</span>
          <select
            required
            value={form.agency}
            onChange={(event) => update('agency', event.target.value)}
          >
            <option value="">{t('Select')}</option>
            {mode.agencies.map(({ id, text }) => (
              <option value={id} key={id}>{t(text)}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>{t(mode.resourceLabel)} *</span>
          <select
            required
            value={form.room}
            onChange={(event) => update('room', event.target.value)}
          >
            <option value="">{t('Select')}</option>
            {mode.resources.filter(({ active = true }) => active).map(({ id, text }) => (
              <option value={id} key={id}>{text}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>{t('Country')}</span>
          <input
            list="country-options"
            value={form.country}
            onChange={(event) => update('country', event.target.value)}
          />
          <datalist id="country-options">
            {countries.map((country) => (
              <option value={country} key={country}>{country}</option>
            ))}
          </datalist>
        </label>

        <label className="field">
          <span>{t('From *')}</span>
          <input
            required
            type="date"
            value={form.startDate}
            onChange={(event) => updateDate('startDate', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('To *')}</span>
          <input
            required
            type="date"
            min={form.startDate || undefined}
            value={form.endDate}
            onChange={(event) => updateDate('endDate', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('Nights')}</span>
          <input readOnly value={nights} />
        </label>

        <label className="field">
          <span>{t('Total (€)')}</span>
          <input
            type="number"
            step="0.01"
            value={form.charge}
            onChange={(event) => updateMoney('charge', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('Commission (€)')}</span>
          <input
            type="number"
            step="0.01"
            value={form.commission}
            onChange={(event) => updateMoney('commission', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('Received (€)')}</span>
          <input
            type="number"
            step="0.01"
            value={form.received}
            onChange={(event) => updateMoney('received', event.target.value)}
          />
        </label>

        <label className="field">
          <span>{t('Status *')}</span>
          <select
            required
            value={form.status}
            onChange={(event) => update('status', event.target.value)}
          >
            {mode.statuses.map(({ id, text }) => (
              <option value={id} key={id}>{t(text)}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>{t('Balance (€)')}</span>
          <input
            readOnly
            value={formatMoney((
              (Number(form.charge) || 0) -
              (Number(form.received) || 0) -
              (Number(form.commission) || 0)
            ), locale)}
          />
        </label>
      </div>
      </fieldset>

      <div className="form-details-toggle">
        <button type="button" className="button button-muted" onClick={() => setShowDetails(!showDetails)}>
          {t(showDetails ? 'Hide details' : 'Show details')}
        </button>
      </div>

      <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minInlineSize: 0 }}>
      {showDetails && (
        <div className="details-grid">
          {detailFields.map(({ name, label, type = 'text' }) => (
            <label className="field" key={name}>
              <span>{t(label)}</span>
              <input
                type={type}
                value={form[name]}
                onChange={(event) => update(name, event.target.value)}
              />
            </label>
          ))}
          <label className="field field-notes">
            <span>{t('Notes')}</span>
            <textarea
              rows="3"
              value={form.extraInfo}
              onChange={(event) => update('extraInfo', event.target.value)}
            />
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
