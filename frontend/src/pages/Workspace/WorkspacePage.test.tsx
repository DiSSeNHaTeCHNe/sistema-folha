import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import WorkspacePage from './WorkspacePage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { listWorkspaces, getWorkspace, createWorkspace } from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  getWorkspace: vi.fn(),
  createWorkspace: vi.fn(),
  deleteWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
}));

describe('WorkspacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([
      { id: 1, nome: 'Planejamento', totalWidgets: 0 },
    ]);
    vi.mocked(getWorkspace).mockResolvedValue({ id: 1, nome: 'Planejamento', widgets: [] });
  });

  it('shows loading spinner initially', () => {
    vi.mocked(listWorkspaces).mockReturnValue(new Promise(() => {}));
    renderWithProviders(<WorkspacePage />);
    expect(screen.getByLabelText('Carregando Workspace')).toBeInTheDocument();
  });

  it('renders heading and switcher for scoped user', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Workspace' })).toBeInTheDocument());
    expect(screen.getByLabelText('Workspace')).toBeInTheDocument();
  });

  it('shows empty state when no workspaces exist', async () => {
    vi.mocked(listWorkspaces).mockResolvedValue([]);
    renderWithProviders(<WorkspacePage />);
    await waitFor(() =>
      expect(screen.getByRole('status', { name: 'Nenhum workspace configurado' })).toBeInTheDocument(),
    );
  });

  it('shows empty workspace alert when layout has no widgets', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/Workspace vazio/i));
  });

  it('shows edit layout button when workspace loaded', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
  });

  it('loads workspace with widgets without crash', async () => {
    vi.mocked(getWorkspace).mockResolvedValue({
      id: 1,
      nome: 'Planejamento',
      widgets: [{ instanceId: 'w1', ordem: 0, colSpan: 4, rowSpan: 1, widgetId: 'kpi-total-funcionarios' }],
    });
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Planejamento', totalWidgets: 1 }]);
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    expect(screen.queryByRole('status', { name: /Workspace vazio/i })).not.toBeInTheDocument();
  });
});

describe('WorkspacePage create flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([]);
    vi.mocked(createWorkspace).mockResolvedValue({ id: 2, nome: 'Novo', widgets: [] });
    vi.mocked(getWorkspace).mockResolvedValue({ id: 2, nome: 'Novo', widgets: [] });
  });

  it('shows empty state before workspaces exist', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() =>
      expect(screen.getByRole('status', { name: 'Nenhum workspace configurado' })).toBeInTheDocument(),
    );
  });
});
