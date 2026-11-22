import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ResourcesPage } from './ResourcesPage.jsx';

const fetchCurrentUser = vi.fn();

vi.mock('../useCurrentUser.js', () => ({
  useCurrentUser: () => ({ currentUser: fetchCurrentUser(), loading: false }),
}));

vi.mock('../bookingApi.js', () => ({
  fetchResources: vi.fn().mockResolvedValue([]),
  createResource: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../i18n.jsx', () => ({
  useLanguage: () => ({ language: 'en', locale: 'en-GB', t: (key, values) => key.replace(/\{(\w+)\}/g, (_, k) => values?.[k] ?? '') }),
}));

vi.mock('../useTheme.js', () => ({ useTheme: () => ({ theme: 'light', toggleTheme: vi.fn() }) }));
vi.mock('./LanguageSelector.jsx', () => ({ LanguageSelector: () => <div /> }));
vi.mock('./ThemeToggle.jsx', () => ({ ThemeToggle: () => <button type="button">theme</button> }));
vi.mock('./HelpButton.jsx', () => ({ HelpButton: () => <button type="button">help</button> }));
vi.mock('./AppSupportActions.jsx', () => ({ AppSupportActions: () => <div /> }));
vi.mock('./FeedbackDialog.jsx', () => ({ FeedbackDialog: () => null }));

describe('ResourcesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows access denied for non-admin role', async () => {
    fetchCurrentUser.mockReturnValue({ role: 'OPERATOR', username: 'operator' });
    render(<ResourcesPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('You do not have permission to perform this action.');
  });
});
