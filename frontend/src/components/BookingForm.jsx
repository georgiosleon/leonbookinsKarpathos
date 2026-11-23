import { useEffect, useRef, useState } from 'react';
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
  const [showDetails, setShowDetails] = useState(false);
  const [showFinancials, setShowFinancials] = useState(true);
  const nights = nightsBetween(form.startDate, form.endDate);
  const { locale, t } = useLanguage();
  const readOnly = !canManage;
  const selectedResource = mode.resources.find((resource) => resource.id === form.resourceId);
  const computedRentalTax = Math.max(0, nights) * (Number(selectedResource?.rentalTaxPerNight) || 0);
  const prevCalcKey = useRef(null);

  useEffect(() => {
    const key = `${form.resourceId}|${form.startDate}|${form.endDate}|${selectedResource?.rentalTaxPerNight ?? 0}`;
    if (prevCalcKey.current === null) {
      prevCalcKey.current = key;
      if (form.rentalTax === '' || form.rentalTax == null) {
        onChange({ ...form, rentalTax: String(computedRentalTax) });
      }
      return;
    }
    if (prevCalcKey.current !== key) {
      prevCalcKey.current = key;
      const next = String(computedRentalTax);
      if (String(form.rentalTax ?? '') !== next) {
        onChange({ ...form, rentalTax: next });
      }
    }
  }, [computedRentalTax, form, onChange, selectedResource?.rentalTaxPerNight]);

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

  const balanceValue = (Number(form.charge) || 0)
    - (Number(form.received) || 0)
    - (Number(form.commission) || 0)
    - (Number(form.rentalTax) || 0);

  return (
    <form className="booking-form" onSubmit={onSubmit}>
      <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minInlineSize: 0 }}>
        <section className="form-section" aria-labelledby="stay-section-heading">
          <div className="panel-heading">
            <div>
              <h3 id="stay-section-heading">{t('Stay / Reservation')}</h3>
              <p>{t('Set dates, resource, status, and guests first.')}</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="field">
              <span>{t('From *')}</span>
              <input required type="date" value={form.startDate} onChange={(event) => updateDate('startDate', event.target.value)} />
            </label>

            <label className="field">
              <span>{t('To *')}</span>
              <input required type="date" min={form.startDate || undefined} value={form.endDate} onChange={(event) => updateDate('endDate', event.target.value)} />
            </label>

            <label className="field">
              <span>{t('Nights')}</span>
              <input readOnly tabIndex={-1} value={nights} />
            </label>

            <label className="field">
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

            <label className="field">
              <span>{t('Guests *')}</span>
              <select required value={form.numOfGuests} onChange={(event) => update('numOfGuests', event.target.value)}>
                <option value="">{t('Select')}</option>
                {guestCounts.map((count) => (
                  <option value={count} key={count}>{count}</option>
                ))}
              </select>
            </label>
          </div>
        </section>

        <section className="form-section" aria-labelledby="guest-section-heading">
          <div className="panel-heading">
            <div>
              <h3 id="guest-section-heading">{t('Guest & Channel')}</h3>
            </div>
          </div>
          <div className="form-grid">
            <label className="field field-name">
              <span>{t('Name *')}</span>
              <input required autoComplete="name" value={form.name} onChange={(event) => update('name', event.target.value)} />
            </label>

            <label className="field">
              <span>{t('Agency / color *')}</span>
              <select required value={form.agency} onChange={(event) => update('agency', event.target.value)}>
                <option value="">{t('Select')}</option>
                {mode.agencies.map(({ id, text }) => (
                  <option value={id} key={id}>{t(text)}</option>
                ))}
              </select>
            </label>

            <label className="field">
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
            <p>{t('Balance = Total − Received − Commission − Rental tax')}</p>
          </div>
          <button type="button" className="button button-muted" onClick={() => setShowFinancials((v) => !v)}>
            {t(showFinancials ? 'Hide financials' : 'Show financials')}
          </button>
        </div>
        {!showFinancials && (
          <p className="form-note">
            {t('Balance (€)')}: {formatMoney(balanceValue, locale)} · {t('Rental tax (€)')}: {formatMoney(Number(form.rentalTax) || 0, locale)}
          </p>
        )}
        {showFinancials && (
          <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minInlineSize: 0 }}>
            <div className="form-grid">
              <label className="field">
                <span>{t('Total (€)')}</span>
                <input type="number" step="0.01" value={form.charge} onChange={(event) => updateMoney('charge', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Commission (€)')}</span>
                <input type="number" step="0.01" value={form.commission} onChange={(event) => updateMoney('commission', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Received (€)')}</span>
                <input type="number" step="0.01" value={form.received} onChange={(event) => updateMoney('received', event.target.value)} />
              </label>

              <label className="field">
                <span>{t('Rental tax per night (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(Number(selectedResource?.rentalTaxPerNight) || 0, locale)} />
              </label>

              <label className="field">
                <span>{t('Rental tax (€)')}</span>
                <input readOnly tabIndex={-1} value={formatMoney(Number(form.rentalTax) || 0, locale)} />
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

      <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minInlineSize: 0 }}>
        {showDetails && (
          <div className="details-grid">
            {detailFields.map(({ name, label, type = 'text' }) => (
              <label className="field" key={name}>
                <span>{t(label)}</span>
                <input type={type} value={form[name]} onChange={(event) => update(name, event.target.value)} />
              </label>
            ))}
            <label className="field field-notes">
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
