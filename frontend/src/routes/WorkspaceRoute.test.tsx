import { beforeEach, describe, expect, it, vi } from 'vitest';
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

function renderWorkspaceRoutes(initialRoute: string) {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/workspace" element={<WorkspaceRoute />}>
          <Route index element={<div>workspace-hub</div>} />
          <Route path="datasets" element={<div>workspace-datasets</div>} />
          <Route path="templates" element={<div>workspace-templates</div>} />
          <Route path=":workspaceId" element={<div>workspace-detail</div>} />
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
    renderWorkspaceRoutes('/workspace');
    expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i);
    expect(screen.queryByText('workspace-hub')).not.toBeInTheDocument();
  });

  it('renders child route for allowed users', () => {
    mockUseAuth.mockReturnValue({ user, loading: false, acessoUsuario: acessoValido });
    renderWorkspaceRoutes('/workspace');
    expect(screen.getByText('workspace-hub')).toBeInTheDocument();
  });

  it('redirects unauthenticated users to login', () => {
    mockUseAuth.mockReturnValue({ user: null, loading: false, acessoUsuario: null });
    renderWorkspaceRoutes('/workspace');
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });
});

describe('WorkspaceRoute v2 route ordering (WKS2-03)', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({ user, loading: false, acessoUsuario: acessoValido });
  });

  it('maps /workspace to hub, not detail', () => {
    renderWorkspaceRoutes('/workspace');
    expect(screen.getByText('workspace-hub')).toBeInTheDocument();
    expect(screen.queryByText('workspace-detail')).not.toBeInTheDocument();
  });

  it('maps /workspace/:workspaceId to detail page', () => {
    renderWorkspaceRoutes('/workspace/42');
    expect(screen.getByText('workspace-detail')).toBeInTheDocument();
  });

  it('does not capture /workspace/datasets as workspaceId', () => {
    renderWorkspaceRoutes('/workspace/datasets');
    expect(screen.getByText('workspace-datasets')).toBeInTheDocument();
    expect(screen.queryByText('workspace-detail')).not.toBeInTheDocument();
  });
});
