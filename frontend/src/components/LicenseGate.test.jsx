import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LicenseGate } from './LicenseGate.jsx';

const api = vi.hoisted(() => ({
  fetchLicenseStatus: vi.fn(),
  fetchCustomers: vi.fn(),
  createCustomer: vi.fn(),
  updateCustomer: vi.fn(),
  deleteCustomer: vi.fn(),
  activateLicense: vi.fn(),
  logout: vi.fn(),
  isAuthRedirectError: (e) => e?.name === 'AuthRedirectError',
}));

vi.mock('../bookingApi.js', () => api);
vi.mock('../i18n.jsx', () => ({ useLanguage: () => ({ t: (k, v) => k.replace(/\{(\w+)\}/g, (_, n) => v?.[n] ?? '') }) }));
vi.mock('../useTheme.js', () => ({ useTheme: () => ({ theme: 'light', toggleTheme: vi.fn() }) }));
vi.mock('./LanguageSelector.jsx', () => ({ LanguageSelector: () => <div /> }));
vi.mock('./ThemeToggle.jsx', () => ({ ThemeToggle: () => <button type="button">theme</button> }));

describe('LicenseGate', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.fetchCustomers.mockResolvedValue([]);
  });

  it('shows blocked message for non-admin', async () => {
    api.fetchLicenseStatus.mockResolvedValue({ activated: false, installId: 'inst-1', customer: null });
    render(<LicenseGate currentUser={{ username: 'op', role: 'OPERATOR' }} onActivated={vi.fn()} />);
    expect(await screen.findByText('License activation required. Please contact an administrator.')).toBeInTheDocument();
    expect(screen.getByDisplayValue('inst-1')).toBeInTheDocument();
    expect(screen.queryByLabelText('License key')).not.toBeInTheDocument();
  });

  it('lets admin create customer and activate', async () => {
    const onActivated = vi.fn();
    api.fetchLicenseStatus
      .mockResolvedValueOnce({ activated: false, installId: 'inst-1', customer: null })
      .mockResolvedValueOnce({ activated: false, installId: 'inst-1', customer: null })
      .mockResolvedValue({ activated: true, installId: 'inst-1', customer: { name: 'Acme' } });
    api.createCustomer.mockResolvedValue({ id: 1, name: 'Acme' });
    api.activateLicense.mockResolvedValue({ activated: true, installId: 'inst-1', customer: { name: 'Acme' } });

    render(<LicenseGate currentUser={{ username: 'admin', role: 'ADMIN' }} onActivated={onActivated} />);
    await screen.findByLabelText('License key');

    await userEvent.type(screen.getByLabelText('Name'), 'Acme');
    await userEvent.click(screen.getByRole('button', { name: 'Create customer' }));
    await waitFor(() => expect(api.createCustomer).toHaveBeenCalledWith({ name: 'Acme', telephone: '', email: '' }));

    await userEvent.type(screen.getByLabelText('License key'), 'signed-key');
    await userEvent.click(screen.getByRole('button', { name: 'Activate' }));
    await waitFor(() => expect(api.activateLicense).toHaveBeenCalledWith('signed-key'));
    await waitFor(() => expect(onActivated).toHaveBeenCalled());
  });

  it('shows error on activation failure', async () => {
    api.fetchLicenseStatus.mockResolvedValue({ activated: false, installId: 'inst-1', customer: null });
    api.activateLicense.mockRejectedValue(new Error('Bad key'));
    render(<LicenseGate currentUser={{ username: 'admin', role: 'ADMIN' }} onActivated={vi.fn()} />);
    await screen.findByLabelText('License key');
    await userEvent.type(screen.getByLabelText('License key'), 'bad');
    await userEvent.click(screen.getByRole('button', { name: 'Activate' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Bad key');
  });

  it('shows load failure', async () => {
    api.fetchLicenseStatus.mockRejectedValue(new Error('boom'));
    render(<LicenseGate currentUser={{ username: 'admin', role: 'ADMIN' }} onActivated={vi.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('boom');
  });

  it('lets admin update and delete customer', async () => {
    api.fetchLicenseStatus.mockResolvedValue({ activated: false, installId: 'inst-1', customer: null });
    api.fetchCustomers
      .mockResolvedValueOnce([{ id: 7, name: 'Old', telephone: '', email: '' }])
      .mockResolvedValueOnce([{ id: 7, name: 'New', telephone: '', email: '' }])
      .mockResolvedValue([]);
    api.updateCustomer.mockResolvedValue({ id: 7, name: 'New' });
    api.deleteCustomer.mockResolvedValue();

    render(<LicenseGate currentUser={{ username: 'admin', role: 'ADMIN' }} onActivated={vi.fn()} />);
    const name = await screen.findByLabelText('Name');
    await userEvent.clear(name);
    await userEvent.type(name, 'New');
    await userEvent.click(screen.getByRole('button', { name: 'Update customer' }));
    await waitFor(() => expect(api.updateCustomer).toHaveBeenCalledWith(7, { name: 'New', telephone: '', email: '' }));

    await screen.findByRole('button', { name: 'Delete customer' });
    await userEvent.click(screen.getByRole('button', { name: 'Delete customer' }));
    await waitFor(() => expect(api.deleteCustomer).toHaveBeenCalledWith(7));
  });

  it('shows activated status after successful activation', async () => {
    api.fetchLicenseStatus.mockResolvedValue({ activated: false, installId: 'inst-1', customer: null });
    api.activateLicense.mockResolvedValue({ activated: true, installId: 'inst-1', customer: { name: 'Acme' } });
    render(<LicenseGate currentUser={{ username: 'admin', role: 'ADMIN' }} onActivated={vi.fn()} />);
    await screen.findByLabelText('License key');
    await userEvent.type(screen.getByLabelText('License key'), 'signed-key');
    await userEvent.click(screen.getByRole('button', { name: 'Activate' }));
    expect(await screen.findByText('Activated for Acme')).toBeInTheDocument();
  });
});
