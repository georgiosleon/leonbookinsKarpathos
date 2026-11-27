import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BookingForm } from './BookingForm.jsx';

vi.mock('../i18n.jsx', () => ({
  useLanguage: () => ({
    locale: 'en-GB',
    t: (key, values) => key.replace(/\{(\w+)\}/g, (_, name) => values?.[name] ?? ''),
  }),
  formatMoney: (value) => String(value),
}));

const mode = {
  resourceLabel: 'Room',
  resources: [{ id: 'APT2', text: 'APT 2', active: true, rentalTaxPerNight: 0 }],
  statuses: [{ id: 'active', text: 'Active' }],
  agencies: [{ id: 'bookingcom', text: 'Booking.com' }],
};

const form = {
  startDate: '',
  endDate: '',
  resourceId: '',
  status: 'active',
  numOfGuests: '',
  name: '',
  agency: '',
  country: '',
  charge: '',
  commission: '',
  received: '',
  extraInfo: '',
  identification: '',
  afm: '',
  email: '',
  tel: '',
  voucher: '',
  taxRefNumber: '',
  invoiceNumber: '',
  platformReservationId: '',
};

const emailText = `Subject: Booking.com: Confirmation for Test Hotel (12 Dec 2026 - 15 Dec 2026) | PIN: 0000

Booking confirmed!
Booking number: 1000000001
PIN code: 0000
Guest name: Test Guest
Property: Test Hotel
Phone: +30 00000 00000
Check-in: Saturday, 12 December 2026 (From 14:00)
Check-out: Tuesday, 15 December 2026 (Until 11:00)
1 x Test Studio — 2 adults
Total price: €276.00
`;

describe('BookingForm email import', () => {
  it('prefills reviewable values without submitting or saving', () => {
    const onChange = vi.fn();
    const onSubmit = vi.fn((event) => event.preventDefault());
    render(
      <BookingForm
        mode={mode}
        form={form}
        onChange={onChange}
        onSubmit={onSubmit}
        bookings={[]}
      />,
    );

    const textarea = screen.getByLabelText('Email text');
    fireEvent.change(textarea, { target: { value: emailText } });
    fireEvent.click(screen.getByRole('button', { name: 'Fill form' }));

    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      platformReservationId: '1000000001',
      name: 'Test Guest',
      startDate: '2026-12-12',
      endDate: '2026-12-15',
      numOfGuests: '2',
      agency: 'bookingcom',
    }));
    expect(onChange.mock.calls[0][0].charge).toBe('');
    expect(onChange.mock.calls[0][0].tel).toBe('');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(textarea.value).toBe('');
    expect(screen.getByText('Property phone ignored (guest telephone left blank).')).toBeInTheDocument();
    expect(screen.getByText('Resource not matched — select it manually.')).toBeInTheDocument();
    expect(screen.getByText('Money found but not saved to the financial fields.')).toBeInTheDocument();

    const nameLabel = screen.getByLabelText('Name *').closest('label');
    expect(nameLabel).toHaveClass('field-imported');
    expect(screen.getByLabelText('From *').closest('label')).toHaveClass('field-imported');
    expect(screen.getByText('Highlighted fields were filled from the email. Review them before saving.')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Name *'), { target: { value: 'Manually corrected' } });
    expect(nameLabel).not.toHaveClass('field-imported');
  });

  it('warns about a duplicate reservation ID', () => {
    render(
      <BookingForm
        mode={mode}
        form={form}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        bookings={[{ id: 7, platformReservationId: '1000000001' }]}
      />,
    );

    fireEvent.change(screen.getByLabelText('Email text'), { target: { value: emailText } });
    fireEvent.click(screen.getByRole('button', { name: 'Fill form' }));

    expect(screen.getByText('This reservation ID already exists on another booking.')).toBeInTheDocument();
  });

  it('hides the import panel for read-only users', () => {
    render(
      <BookingForm
        mode={mode}
        form={form}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        canManage={false}
      />,
    );

    expect(screen.queryByRole('button', { name: 'Fill form' })).not.toBeInTheDocument();
  });
});
