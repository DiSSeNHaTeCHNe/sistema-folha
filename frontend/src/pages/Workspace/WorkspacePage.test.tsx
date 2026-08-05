/** @deprecated v1 monolith — scenarios migrated to WorkspaceHubPage.test and WorkspaceDetailPage.test (T28) */
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import WorkspacePage from './WorkspacePage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { listWorkspaces, getWorkspace } from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  getWorkspace: vi.fn(),
  createWorkspace: vi.fn(),
  deleteWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
  listWidgetDefinitions: vi.fn().mockResolvedValue([]),
  installOrcamentoTemplate: vi.fn(),
  createWorkspaceProposal: vi.fn(),
  confirmWorkspaceProposal: vi.fn(),
  discardWorkspaceProposal: vi.fn(),
}));

describe('WorkspacePage (deprecated v1 smoke)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([
      { id: 1, nome: 'Planejamento', totalWidgets: 0 },
    ]);
    vi.mocked(getWorkspace).mockResolvedValue({ id: 1, nome: 'Planejamento', widgets: [] });
  });

  it('still renders for v1 regression baseline', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Workspace' })).toBeInTheDocument());
  });

  it('does not reference WidgetBuilderDrawer in toolbar', async () => {
    renderWithProviders(<WorkspacePage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Novo widget' })).not.toBeInTheDocument();
  });
});
