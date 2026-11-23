import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReportsPage } from './ReportsPage.jsx';

const fetchCurrentUser = vi.fn();

vi.mock('../useCurrentUser.js', () => ({
  useCurrentUser: () => ({ currentUser: fetchCurrentUser(), loading: false }),
}));

vi.mock('../bookingApi.js', () => ({
  fetchBookings: vi.fn().mockResolvedValue([]),
  fetchResources: vi.fn().mockResolvedValue([]),
  fetchBookingReport: vi.fn(),
  bookingPdfUrl: vi.fn(),
  logout: vi.fn(),
  isAuthRedirectError: (error) => error?.name === 'AuthRedirectError',
}));

vi.mock('../i18n.jsx', () => ({
  useLanguage: () => ({ language: 'en', locale: 'en-GB', t: (key, values) => key.replace(/\{(\w+)\}/g, (_, k) => values?.[k] ?? '') }),
  formatMoney: (v) => String(v),
}));

vi.mock('../useTheme.js', () => ({ useTheme: () => ({ theme: 'light', toggleTheme: vi.fn() }) }));
vi.mock('./LanguageSelector.jsx', () => ({ LanguageSelector: () => <div /> }));
vi.mock('./ThemeToggle.jsx', () => ({ ThemeToggle: () => <button type="button">theme</button> }));
vi.mock('./HelpButton.jsx', () => ({ HelpButton: () => <button type="button">help</button> }));
vi.mock('./AppSupportActions.jsx', () => ({ AppSupportActions: () => <div /> }));
vi.mock('./FeedbackDialog.jsx', () => ({ FeedbackDialog: () => null }));

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows access denied for guest role', async () => {
    fetchCurrentUser.mockReturnValue({ role: 'GUEST', username: 'guest' });
    render(<ReportsPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('You do not have permission to perform this action.');
  });
});
