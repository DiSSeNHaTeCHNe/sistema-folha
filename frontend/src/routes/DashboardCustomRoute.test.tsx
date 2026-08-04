import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { DashboardCustomRoute } from './DashboardCustomRoute';
import type { AcessoUsuario, Usuario } from '../types';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

const user: Usuario = { id: 1, login: 'user', nome: 'User', permissoes: ['USER'] };

const acessoValido: AcessoUsuario = {
  temFuncionarioVinculado: true,
  temNoOrganograma: true,
  acessoTotal: false,
  centrosCustoIds: [10],
  quantidadeCentrosAcessiveis: 1,
};

function renderRoute(initialRoute = '/meu-dashboard') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/dashboard" element={<div>dashboard-page</div>} />
        <Route path="/meu-dashboard" element={<DashboardCustomRoute />}>
          <Route index element={<div>meu-dashboard-content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('DashboardCustomRoute', () => {
  it('redirects users without dashboard scope', () => {
    mockUseAuth.mockReturnValue({
      user,
      loading: false,
      acessoUsuario: { ...acessoValido, centrosCustoIds: [] },
    });
    renderRoute();
    expect(screen.getByText('dashboard-page')).toBeInTheDocument();
  });

  it('renders child route for allowed users', () => {
    mockUseAuth.mockReturnValue({ user, loading: false, acessoUsuario: acessoValido });
    renderRoute();
    expect(screen.getByText('meu-dashboard-content')).toBeInTheDocument();
  });
});
