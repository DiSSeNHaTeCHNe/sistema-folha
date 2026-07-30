import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Login } from './index';
import { defaultMockAuth, renderWithProviders } from '../../test/renderWithProviders';

const mockNavigate = vi.fn();
const mockLogin = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    ...defaultMockAuth,
    login: mockLogin,
  }),
}));

describe('Login page', () => {
  it('renders the main heading', () => {
    renderWithProviders(<Login />);

    expect(screen.getByRole('heading', { name: 'Sistema de Folha' })).toBeInTheDocument();
  });

  it('shows login and password fields by label', () => {
    renderWithProviders(<Login />);

    expect(screen.getByRole('textbox', { name: /login/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/^Senha/)).toBeInTheDocument();
  });

  it('submits credentials through auth login', async () => {
    mockLogin.mockResolvedValue(undefined);

    renderWithProviders(<Login />);

    fireEvent.change(screen.getByRole('textbox', { name: /login/i }), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText(/^Senha/), { target: { value: 'secret' } });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({ login: 'admin', senha: 'secret' });
    });
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
