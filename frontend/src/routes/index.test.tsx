import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { routeObjects } from './index';
import {
  defaultMockAuth,
  TestAuthProvider,
  type MockAuthContextValue,
} from '../test/renderWithProviders';
import { criarTema, TEMA_PADRAO } from '../theme/themes';
import type { AcessoUsuario, Usuario } from '../types';

vi.mock('../components/Layout', () => ({
  Layout: () => <Outlet />,
}));

vi.mock('../pages/Dashboard', () => ({
  default: () => <h1>Dashboard Gerencial</h1>,
}));

vi.mock('../services/workspaceService', () => ({
  listWorkspaces: vi.fn().mockResolvedValue([]),
  listDatasets: vi.fn().mockResolvedValue([]),
  listWidgetDefinitions: vi.fn().mockResolvedValue([]),
  createWorkspace: vi.fn(),
}));

vi.mock('../contexts/AuthContext', async () => {
  const testModule = await vi.importActual<typeof import('../test/renderWithProviders')>(
    '../test/renderWithProviders',
  );
  return {
    AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    useAuth: () => {
      const ctx = testModule.useTestAuth();
      return {
        ...ctx,
        isAuthenticated: !!ctx.user,
      };
    },
  };
});

const user: Usuario = { id: 1, login: 'user', nome: 'User', permissoes: ['USER'] };

const acessoValido: AcessoUsuario = {
  temFuncionarioVinculado: true,
  temNoOrganograma: true,
  acessoTotal: false,
  centrosCustoIds: [10],
  quantidadeCentrosAcessiveis: 1,
};

function renderRoutes(initialEntry: string, authContext: Partial<MockAuthContextValue> = {}) {
  const authValue: MockAuthContextValue = {
    ...defaultMockAuth,
    user,
    isAuthenticated: true,
    acessoUsuario: acessoValido,
    ...authContext,
  };
  const router = createMemoryRouter(routeObjects, { initialEntries: [initialEntry] });

  return render(
    <ThemeProvider theme={criarTema(TEMA_PADRAO)}>
      <TestAuthProvider value={authValue}>
        <RouterProvider router={router} />
      </TestAuthProvider>
    </ThemeProvider>,
  );
}

describe('routeObjects data router smoke (WKS2F2-05)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders dashboard at /dashboard for authenticated user', async () => {
    renderRoutes('/dashboard');
    expect(await screen.findByRole('heading', { name: 'Dashboard Gerencial', level: 1 })).toBeInTheDocument();
  });

  it('renders workspace hub at /workspace for allowed user', async () => {
    renderRoutes('/workspace');
    expect(await screen.findByRole('heading', { name: 'Meus workspaces', level: 1 })).toBeInTheDocument();
  });
});

describe('routeObjects auth parity (WKS2F2-06)', () => {
  it('redirects unauthenticated private route access to login', async () => {
    renderRoutes('/dashboard', { user: null, isAuthenticated: false, acessoUsuario: null });
    expect(await screen.findByRole('heading', { name: 'Sistema de Folha', level: 1 })).toBeInTheDocument();
  });
});

describe('routeObjects workspace ACL parity (WKS2F2-07)', () => {
  it('shows access denied alert when user lacks workspace scope', async () => {
    renderRoutes('/workspace', {
      acessoUsuario: { ...acessoValido, centrosCustoIds: [], quantidadeCentrosAcessiveis: 0 },
    });
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i),
    );
    expect(screen.queryByRole('heading', { name: 'Meus workspaces' })).not.toBeInTheDocument();
  });
});
