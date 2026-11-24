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
});
