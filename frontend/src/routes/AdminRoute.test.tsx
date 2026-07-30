import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AdminRoute } from './AdminRoute';
import type { Usuario } from '../types';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

const adminUser: Usuario = {
  id: 1,
  login: 'admin',
  nome: 'Admin',
  permissoes: ['ADMIN'],
};

const regularUser: Usuario = {
  id: 2,
  login: 'user',
  nome: 'User',
  permissoes: ['USER'],
};

function renderAdminRoute(initialRoute = '/admin') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/dashboard" element={<div>dashboard-page</div>} />
        <Route path="/admin" element={<AdminRoute />}>
          <Route index element={<div>admin-content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminRoute', () => {
  it('shows loading indicator while auth is loading', () => {
    mockUseAuth.mockReturnValue({ user: null, loading: true });

    renderAdminRoute();

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('redirects unauthenticated users to login', () => {
    mockUseAuth.mockReturnValue({ user: null, loading: false });

    renderAdminRoute();

    expect(screen.getByText('login-page')).toBeInTheDocument();
  });

  it('redirects non-admin users to dashboard', () => {
    mockUseAuth.mockReturnValue({ user: regularUser, loading: false });

    renderAdminRoute();

    expect(screen.getByText('dashboard-page')).toBeInTheDocument();
  });

  it('renders child routes for admin users', () => {
    mockUseAuth.mockReturnValue({ user: adminUser, loading: false });

    renderAdminRoute();

    expect(screen.getByText('admin-content')).toBeInTheDocument();
  });
});
