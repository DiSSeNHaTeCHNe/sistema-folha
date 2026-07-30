import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ApiKeyRoute } from './ApiKeyRoute';
import type { Usuario } from '../types';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

const apiKeyUser: Usuario = {
  id: 1,
  login: 'apiuser',
  nome: 'API User',
  permissoes: ['API_KEY'],
};

const adminUser: Usuario = {
  id: 2,
  login: 'admin',
  nome: 'Admin',
  permissoes: ['ADMIN'],
};

const regularUser: Usuario = {
  id: 3,
  login: 'user',
  nome: 'User',
  permissoes: ['USER'],
};

function renderApiKeyRoute(initialRoute = '/api-keys') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/dashboard" element={<div>dashboard-page</div>} />
        <Route path="/api-keys" element={<ApiKeyRoute />}>
          <Route index element={<div>api-keys-content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ApiKeyRoute', () => {
  it('shows loading indicator while auth is loading', () => {
    mockUseAuth.mockReturnValue({ user: null, loading: true });

    renderApiKeyRoute();

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('redirects users without API_KEY or ADMIN to dashboard', () => {
    mockUseAuth.mockReturnValue({ user: regularUser, loading: false });

    renderApiKeyRoute();

    expect(screen.getByText('dashboard-page')).toBeInTheDocument();
  });

  it('renders child routes for users with API_KEY permission', () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });

    renderApiKeyRoute();

    expect(screen.getByText('api-keys-content')).toBeInTheDocument();
  });

  it('renders child routes for ADMIN users without API_KEY permission', () => {
    mockUseAuth.mockReturnValue({ user: adminUser, loading: false });

    renderApiKeyRoute();

    expect(screen.getByText('api-keys-content')).toBeInTheDocument();
  });
});
