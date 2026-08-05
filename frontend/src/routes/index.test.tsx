import { beforeEach, describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { render, screen, waitFor } from '@testing-library/react';
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { routeObjects, RouterWithAuth } from './index';
import {
  defaultMockAuth,
  TestAuthProvider,
  type MockAuthContextValue,
} from '../test/renderWithProviders';
import { criarTema, TEMA_PADRAO } from '../theme/themes';
import type { AcessoUsuario, Usuario } from '../types';

const { createBrowserRouterMock } = vi.hoisted(() => ({
  createBrowserRouterMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return {
    ...actual,
    createBrowserRouter: (...args: Parameters<typeof actual.createBrowserRouter>) => {
      createBrowserRouterMock(...args);
      return actual.createBrowserRouter(...args);
    },
  };
});

const routesModuleSource = readFileSync(
  resolve(dirname(fileURLToPath(import.meta.url)), 'index.tsx'),
  'utf-8',
);

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

vi.mock('../services/usuarioService', () => ({
  default: {
    listar: vi.fn().mockResolvedValue([]),
    listarFuncionarios: vi.fn().mockResolvedValue([]),
    criar: vi.fn(),
    atualizar: vi.fn(),
    excluir: vi.fn(),
  },
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
const adminUser: Usuario = { id: 2, login: 'admin', nome: 'Admin', permissoes: ['ADMIN'] };

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

describe('RouterWithAuth production data router (WKS2F2-04)', () => {
  beforeEach(() => {
    createBrowserRouterMock.mockClear();
  });

  it('wires routeObjects through createBrowserRouter in production module', () => {
    expect(routesModuleSource).toMatch(/createBrowserRouter\s*\(\s*routeObjects\s*\)/);
    expect(routesModuleSource).not.toMatch(/<\s*BrowserRouter/);
    expect(routesModuleSource).not.toMatch(/import\s*{[^}]*\bBrowserRouter\b/);
  });

  it('mounts RouterProvider with router from createBrowserRouter (not BrowserRouter)', async () => {
    render(
      <ThemeProvider theme={criarTema(TEMA_PADRAO)}>
        <TestAuthProvider
          value={{
            ...defaultMockAuth,
            user,
            isAuthenticated: true,
            acessoUsuario: acessoValido,
          }}
        >
          <RouterWithAuth />
        </TestAuthProvider>
      </ThemeProvider>,
    );

    expect(createBrowserRouterMock).toHaveBeenCalledWith(routeObjects);
    expect(
      await screen.findByRole('heading', { name: 'Dashboard Gerencial', level: 1 }),
    ).toBeInTheDocument();
  });
});

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

  it('renders login at /login', async () => {
    renderRoutes('/login', { user: null, isAuthenticated: false, acessoUsuario: null });
    expect(await screen.findByRole('heading', { name: 'Sistema de Folha', level: 1 })).toBeInTheDocument();
  });

  it('renders dataset list at /workspace/datasets for allowed user', async () => {
    renderRoutes('/workspace/datasets');
    expect(await screen.findByRole('heading', { name: 'Datasets', level: 1 })).toBeInTheDocument();
  });

  it('renders usuarios admin page at /usuarios for admin user', async () => {
    renderRoutes('/usuarios', { user: adminUser });
    expect(
      await screen.findByRole('heading', { name: 'Manutenção de Usuários' }),
    ).toBeInTheDocument();
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
