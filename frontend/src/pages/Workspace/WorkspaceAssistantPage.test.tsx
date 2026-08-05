import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import WorkspaceAssistantPage from './WorkspaceAssistantPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  confirmWorkspaceProposal,
  createWorkspaceProposal,
  discardWorkspaceProposal,
  listWorkspaces,
} from '../../services/workspaceService';
import type { WorkspaceProposal } from './types';

const mockUseAuth = vi.fn();

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../services/workspaceService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/workspaceService')>();
  return {
    ...actual,
    listWorkspaces: vi.fn(),
    createWorkspaceProposal: vi.fn(),
    confirmWorkspaceProposal: vi.fn(),
    discardWorkspaceProposal: vi.fn(),
  };
});

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

describe('WorkspaceAssistantPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Principal', totalWidgets: 0 }]);
    mockUseAuth.mockReturnValue({
      user: { id: 1, login: 'ia-user', nome: 'IA User', permissoes: ['WORKSPACE_IA_CRIAR'] },
    });
  });

  it('shows unavailable message without WORKSPACE_IA_CRIAR (WKS2-32)', () => {
    mockUseAuth.mockReturnValue({
      user: { id: 2, login: 'user', nome: 'User', permissoes: [] },
    });
    renderWithProviders(<WorkspaceAssistantPage />);
    expect(screen.getByRole('status')).toHaveTextContent(/não possui permissão/i);
    expect(screen.queryByRole('button', { name: 'Gerar proposta' })).not.toBeInTheDocument();
  });

  it('renders assistant form when user has permission (WKS2-31)', () => {
    renderWithProviders(<WorkspaceAssistantPage />);
    expect(screen.getByRole('heading', { name: 'Assistente IA', level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText('O que você deseja criar?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Gerar proposta' })).toBeDisabled();
  });

  it('shows pending proposal with 72h TTL after generate (WKS2-34)', async () => {
    vi.mocked(createWorkspaceProposal).mockResolvedValue(pendingProposal);
    renderWithProviders(<WorkspaceAssistantPage />);
    fireEvent.change(screen.getByLabelText('O que você deseja criar?'), {
      target: { value: 'KPI de folha' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar proposta' }));

    await waitFor(() =>
      expect(screen.getByText(/TTL de 72 horas/i)).toBeInTheDocument(),
    );
    expect(screen.getByText(/Resumo folha/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeInTheDocument();
  });

  it('discards proposal without persisting until confirm (WKS2-31)', async () => {
    vi.mocked(createWorkspaceProposal).mockResolvedValue(pendingProposal);
    vi.mocked(discardWorkspaceProposal).mockResolvedValue(undefined);
    renderWithProviders(<WorkspaceAssistantPage />);
    fireEvent.change(screen.getByLabelText('O que você deseja criar?'), {
      target: { value: 'KPI de folha' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar proposta' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Descartar' }));

    await waitFor(() => expect(discardWorkspaceProposal).toHaveBeenCalledWith(42));
    expect(confirmWorkspaceProposal).not.toHaveBeenCalled();
  });
});
