import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { WorkspaceRoute } from './WorkspaceRoute';
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

function renderRoute(initialRoute = '/workspace') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/workspace" element={<WorkspaceRoute />}>
          <Route index element={<div>workspace-content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('WorkspaceRoute', () => {
  it('shows access denied alert for users without scope', () => {
    mockUseAuth.mockReturnValue({
      user,
      loading: false,
      acessoUsuario: { ...acessoValido, centrosCustoIds: [] },
    });
    renderRoute();
    expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i);
    expect(screen.queryByText('workspace-content')).not.toBeInTheDocument();
  });

  it('renders child route for allowed users', () => {
    mockUseAuth.mockReturnValue({ user, loading: false, acessoUsuario: acessoValido });
    renderRoute();
    expect(screen.getByText('workspace-content')).toBeInTheDocument();
  });

  it('redirects unauthenticated users to login', () => {
    mockUseAuth.mockReturnValue({ user: null, loading: false, acessoUsuario: null });
    renderRoute();
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });
});
