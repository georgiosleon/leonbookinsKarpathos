import { useState } from 'react';
import { guestCounts } from '../bookingConfig.js';
import countries from '../countries.json';
import { nightsBetween } from '../bookingLogic.js';

const detailFields = [
  { name: 'identification', label: 'Passport / ID' },
  { name: 'afm', label: 'Tax number (ΑΦΜ)' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'tel', label: 'Telephone', type: 'tel' },
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
}) {
  const [showDetails, setShowDetails] = useState(true);
  const nights = nightsBetween(form.startDate, form.endDate);

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
      <div className="form-grid">
        <label className="field field-name">
          <span>Name *</span>
          <input
            required
            autoComplete="name"
            value={form.name}
            onChange={(event) => update('name', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Guests *</span>
          <select
            required
            value={form.numOfGuests}
            onChange={(event) => update('numOfGuests', event.target.value)}
          >
            <option value="">Select</option>
            {guestCounts.map((count) => (
              <option value={count} key={count}>{count}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Agency / color *</span>
          <select
            required
            value={form.agency}
            onChange={(event) => update('agency', event.target.value)}
          >
            <option value="">Select</option>
            {mode.agencies.map(({ id, text }) => (
              <option value={id} key={id}>{text}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>{mode.resourceLabel} *</span>
          <select
            required
            value={form.room}
            onChange={(event) => update('room', event.target.value)}
          >
            <option value="">Select</option>
            {mode.resources.map(({ id, text }) => (
              <option value={id} key={id}>{text}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Country</span>
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
          <span>From *</span>
          <input
            required
            type="date"
            value={form.startDate}
            onChange={(event) => updateDate('startDate', event.target.value)}
          />
        </label>

        <label className="field">
          <span>To *</span>
          <input
            required
            type="date"
            min={form.startDate || undefined}
            value={form.endDate}
            onChange={(event) => updateDate('endDate', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Nights</span>
          <input readOnly value={nights} />
        </label>

        <label className="field">
          <span>Total (€)</span>
          <input
            type="number"
            step="0.01"
            value={form.charge}
            onChange={(event) => updateMoney('charge', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Commission (€)</span>
          <input
            type="number"
            step="0.01"
            value={form.commission}
            onChange={(event) => updateMoney('commission', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Received (€)</span>
          <input
            type="number"
            step="0.01"
            value={form.received}
            onChange={(event) => updateMoney('received', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Status *</span>
          <select
            required
            value={form.status}
            onChange={(event) => update('status', event.target.value)}
          >
            {mode.statuses.map(({ id, text }) => (
              <option value={id} key={id}>{text}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Balance (€)</span>
          <input
            readOnly
            value={(
              (Number(form.charge) || 0) -
              (Number(form.received) || 0) -
              (Number(form.commission) || 0)
            ).toFixed(2)}
          />
        </label>
      </div>

      <div className="form-details-toggle">
        <button type="button" className="button button-muted" onClick={() => setShowDetails(!showDetails)}>
          {showDetails ? 'Hide details' : 'Show details'}
        </button>
      </div>

      {showDetails && (
        <div className="details-grid">
          {detailFields.map(({ name, label, type = 'text' }) => (
            <label className="field" key={name}>
              <span>{label}</span>
              <input
                type={type}
                value={form[name]}
                onChange={(event) => update(name, event.target.value)}
              />
            </label>
          ))}
          <label className="field field-notes">
            <span>Notes</span>
            <textarea
              rows="3"
              value={form.extraInfo}
              onChange={(event) => update('extraInfo', event.target.value)}
            />
          </label>
        </div>
      )}

      {selectedBooking && (
        <p className="form-note">
          Selected booking {selectedBooking.name} ({selectedBooking.id}).{' '}
          {saveMode === 'edit'
            ? 'Saving updates this booking.'
            : 'Saving creates a new booking and leaves this one unchanged.'}
        </p>
      )}

      <div className="form-actions">
        <button className="button button-primary" type="submit" disabled={busy || nights <= 0}>
          {busy
            ? (saveMode === 'edit' && selectedBooking ? 'Updating…' : 'Saving…')
            : (saveMode === 'edit' && selectedBooking ? 'Update booking' : 'Save as new booking')}
        </button>
        <button className="button button-muted" type="button" onClick={onReset} disabled={busy}>
          New / reset
        </button>
      </div>
    </form>
  );
}
