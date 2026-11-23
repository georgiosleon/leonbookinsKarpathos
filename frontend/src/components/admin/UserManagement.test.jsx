import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UserManagement } from './UserManagement.jsx';

const fetchUsers = vi.fn();
const createUser = vi.fn();
const updateUser = vi.fn();
const deleteUser = vi.fn();
const logout = vi.fn();

vi.mock('../../usersApi.js', () => ({
  fetchUsers: (...args) => fetchUsers(...args),
  createUser: (...args) => createUser(...args),
  updateUser: (...args) => updateUser(...args),
  deleteUser: (...args) => deleteUser(...args),
}));

vi.mock('../../bookingApi.js', () => ({
  logout: (...args) => logout(...args),
  isAuthRedirectError: (error) => error?.name === 'AuthRedirectError',
}));

vi.mock('../../i18n.jsx', () => ({
  useLanguage: () => ({ language: 'en', t: (key, values) => key.replace(/\{(\w+)\}/g, (_, k) => values?.[k] ?? '') }),
}));

vi.mock('../../useTheme.js', () => ({
  useTheme: () => ({ theme: 'light', toggleTheme: vi.fn() }),
}));

vi.mock('../LanguageSelector.jsx', () => ({ LanguageSelector: () => <div /> }));
vi.mock('../ThemeToggle.jsx', () => ({ ThemeToggle: () => <button type="button">theme</button> }));
vi.mock('../HelpButton.jsx', () => ({ HelpButton: () => <button type="button">help</button> }));
vi.mock('../AppSupportActions.jsx', () => ({ AppSupportActions: () => <div /> }));
vi.mock('../FeedbackDialog.jsx', () => ({ FeedbackDialog: ({ message }) => (message ? <div role="dialog">{message}</div> : null) }));

describe('UserManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders users loaded from the API', async () => {
    fetchUsers.mockResolvedValue([
      { id: 1, username: 'admin', email: 'admin@test.com', role: 'ADMIN', active: true },
    ]);

    render(<UserManagement />);

    expect(await screen.findByText('admin')).toBeInTheDocument();
    expect(screen.getByText('admin@test.com')).toBeInTheDocument();
  });

  it('shows access denied / load error when user list cannot be loaded', async () => {
    fetchUsers.mockRejectedValue(new Error('You do not have permission to perform this action.'));

    render(<UserManagement />);

    expect(await screen.findByRole('alert')).toHaveTextContent('You do not have permission to perform this action.');
  });

  it('creates a user with the password field', async () => {
    fetchUsers.mockResolvedValue([]);
    createUser.mockResolvedValue({
      id: 2, username: 'alice', email: 'alice@test.com', role: 'OPERATOR', active: true,
    });

    render(<UserManagement />);
    await screen.findByText('No users found.');

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Username *'), 'alice');
    await user.type(screen.getByLabelText('Email'), 'alice@test.com');
    await user.type(screen.getByLabelText('Password *'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Add user' }));

    await waitFor(() => {
      expect(createUser).toHaveBeenCalledWith({
        username: 'alice',
        email: 'alice@test.com',
        role: 'OPERATOR',
        active: true,
        password: 'password123',
      });
    });
    expect(await screen.findByText('User added.')).toBeInTheDocument();
  });
});
