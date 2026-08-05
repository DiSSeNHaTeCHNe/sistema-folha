import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import WorkspaceSuggestionsPage from './WorkspaceSuggestionsPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  confirmWorkspaceProposal,
  createWorkspaceProposal,
  discardWorkspaceProposal,
  getWorkspace,
  listWorkspaces,
} from '../../services/workspaceService';
import type { WorkspaceProposal } from './types';

vi.mock('../../services/workspaceService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/workspaceService')>();
  return {
    ...actual,
    getWorkspace: vi.fn(),
    listWorkspaces: vi.fn(),
    createWorkspaceProposal: vi.fn(),
    confirmWorkspaceProposal: vi.fn(),
    discardWorkspaceProposal: vi.fn(),
  };
});

const pendingProposal: WorkspaceProposal = {
  id: 55,
  status: 'PENDENTE',
  solicitanteUsuarioId: 1,
  dataCriacao: '2026-08-05T10:00:00',
  dataExpiracao: '2026-08-08T10:00:00',
  dataResolucao: null,
  payload: {
    kind: 'WIDGET',
    nome: 'Sugestão KPI',
    tipoWidget: 'KPI',
    formula: 'SOMA(valor)',
    descricao: 'Sugestão automática',
  },
};

function renderSuggestions(route = '/workspace/1/sugestoes') {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/:workspaceId/sugestoes" element={<WorkspaceSuggestionsPage />} />
    </Routes>,
    { route },
  );
}

describe('WorkspaceSuggestionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getWorkspace).mockResolvedValue({ id: 1, nome: 'Planejamento', widgets: [] });
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Planejamento', totalWidgets: 0 }]);
    vi.mocked(createWorkspaceProposal).mockResolvedValue(pendingProposal);
  });

  it('auto-generates SUGESTAO proposal on mount (WKS2-33)', async () => {
    renderSuggestions();
    await waitFor(() =>
      expect(createWorkspaceProposal).toHaveBeenCalledWith('SUGESTAO'),
    );
    expect(await screen.findByText(/Sugestão KPI/)).toBeInTheDocument();
  });

  it('shows manual review message — never auto-applied (WKS2-33)', async () => {
    renderSuggestions();
    await waitFor(() =>
      expect(screen.getByText(/nunca são aplicadas automaticamente/i)).toBeInTheDocument(),
    );
    expect(confirmWorkspaceProposal).not.toHaveBeenCalled();
  });

  it('requires explicit confirm to apply suggestion', async () => {
    vi.mocked(confirmWorkspaceProposal).mockResolvedValue({ ...pendingProposal, status: 'APLICADA' });
    renderSuggestions();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
    await waitFor(() =>
      expect(confirmWorkspaceProposal).toHaveBeenCalledWith(55, { workspaceId: 1 }),
    );
  });

  it('allows discard without applying', async () => {
    vi.mocked(discardWorkspaceProposal).mockResolvedValue(undefined);
    renderSuggestions();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Descartar' }));
    await waitFor(() => expect(discardWorkspaceProposal).toHaveBeenCalledWith(55));
  });
});
