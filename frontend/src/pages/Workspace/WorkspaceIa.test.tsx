import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import WorkspacePage from './WorkspacePage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { createAuthMswServer } from '../../test/mswServer';
import { API_BASE_URL } from '../../test/handlers/authHandlers';
import type { WorkspaceProposal } from './types';

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 1,
      login: 'ia-user',
      nome: 'IA User',
      permissoes: ['WORKSPACE_IA_CRIAR'],
    },
    loading: false,
    login: async () => {},
    logout: () => {},
    isAuthenticated: true,
    acessoUsuario: {
      acessoTotal: false,
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      centrosCustoIds: [1],
    },
    podeAcessarCentroCusto: () => true,
  }),
}));

vi.mock('../../services/workspaceService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/workspaceService')>();
  return {
    ...actual,
    listWorkspaces: vi.fn().mockResolvedValue([{ id: 1, nome: 'Principal', totalWidgets: 0 }]),
    getWorkspace: vi.fn().mockResolvedValue({ id: 1, nome: 'Principal', widgets: [] }),
    listWidgetDefinitions: vi.fn().mockResolvedValue([]),
    deleteWorkspace: vi.fn(),
    saveWorkspaceLayout: vi.fn(),
    createWorkspace: vi.fn(),
    installOrcamentoTemplate: vi.fn(),
  };
});

const server = createAuthMswServer();

const pendingProposal: WorkspaceProposal = {
  id: 42,
  status: 'PENDENTE',
  solicitanteUsuarioId: 1,
  dataCriacao: '2026-08-05T10:00:00',
  dataExpiracao: '2026-08-08T10:00:00',
  dataResolucao: null,
  payload: {
    kind: 'WIDGET',
    nome: 'Resumo folha',
    tipoWidget: 'KPI',
    formula: 'SOMA(total_liquido)',
    descricao: 'Widget KPI sugerido',
  },
};

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Workspace IA flows', () => {
  it('shows Sugerir para mim when user has WORKSPACE_IA_CRIAR', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Sugerir para mim' })).toBeInTheDocument(),
    );
  });

  it('opens review dialog after suggest creates proposal via MSW', async () => {
    server.use(
      http.post(`${API_BASE_URL}/workspace/proposals`, async ({ request }) => {
        const body = await request.json() as { tipo?: string };
        expect(body.tipo).toBe('SUGESTAO');
        return HttpResponse.json(pendingProposal, { status: 201 });
      }),
    );

    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sugerir para mim' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Sugerir para mim' }));

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Revisar proposta' })).toBeInTheDocument(),
    );
    expect(screen.getByText(/Resumo folha/)).toBeInTheDocument();
  });

  it('confirm proposal calls MSW confirm endpoint', async () => {
    let confirmed = false;
    server.use(
      http.post(`${API_BASE_URL}/workspace/proposals`, () =>
        HttpResponse.json(pendingProposal, { status: 201 })),
      http.post(`${API_BASE_URL}/workspace/proposals/42/confirmar`, async () => {
        confirmed = true;
        return HttpResponse.json({ ...pendingProposal, status: 'APLICADA' });
      }),
    );

    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sugerir para mim' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Sugerir para mim' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(confirmed).toBe(true));
    await waitFor(() =>
      expect(screen.getByText('Proposta aplicada com sucesso')).toBeInTheDocument(),
    );
  });

  it('discard proposal calls MSW discard endpoint', async () => {
    let discarded = false;
    server.use(
      http.post(`${API_BASE_URL}/workspace/proposals`, () =>
        HttpResponse.json(pendingProposal, { status: 201 })),
      http.post(`${API_BASE_URL}/workspace/proposals/42/descartar`, () => {
        discarded = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sugerir para mim' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Sugerir para mim' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Descartar' }));

    await waitFor(() => expect(discarded).toBe(true));
    await waitFor(() =>
      expect(screen.getByText('Proposta descartada')).toBeInTheDocument(),
    );
  });

  it('shows permission denied error from MSW 403 on suggest', async () => {
    server.use(
      http.post(`${API_BASE_URL}/workspace/proposals`, () =>
        HttpResponse.json({ status: 403, message: 'Capacidade não disponível' }, { status: 403 })),
    );

    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sugerir para mim' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Sugerir para mim' }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Capacidade não disponível'),
    );
  });

  it('does not auto-suggest on page load', async () => {
    let createCalled = false;
    server.use(
      http.post(`${API_BASE_URL}/workspace/proposals`, () => {
        createCalled = true;
        return HttpResponse.json(pendingProposal, { status: 201 });
      }),
    );

    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Workspace' })).toBeInTheDocument());
    expect(createCalled).toBe(false);
    expect(screen.queryByRole('heading', { name: 'Revisar proposta' })).not.toBeInTheDocument();
  });
});
