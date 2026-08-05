import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import WorkspaceDetailPage from './WorkspaceDetailPage';
import { renderWithDataRouter } from '../../test/renderWithDataRouter';
import { getWorkspace, listWorkspaces, listWidgetDefinitions } from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  getWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
  listWidgetDefinitions: vi.fn().mockResolvedValue([]),
  installOrcamentoTemplate: vi.fn(),
  createWidgetDefinition: vi.fn(),
}));

vi.mock('./WorkspaceGrid', () => ({
  WorkspaceGrid: () => (
    <div role="region" aria-label="Grid de widgets do workspace">
      grid
    </div>
  ),
}));

function renderDetail(route = '/workspace/1') {
  return renderWithDataRouter(<WorkspaceDetailPage />, {
    routes: [{ path: '/workspace/:workspaceId', element: <WorkspaceDetailPage /> }],
    initialEntries: [route],
  });
}

describe('WorkspaceDetailPage integration under data router', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Planejamento', totalWidgets: 1 }]);
    vi.mocked(getWorkspace).mockResolvedValue({
      id: 1,
      nome: 'Planejamento',
      widgets: [{ instanceId: 'w1', ordem: 0, colSpan: 4, rowSpan: 1, widgetId: 'kpi-total-funcionarios' }],
    });
    vi.mocked(listWidgetDefinitions).mockResolvedValue([]);
  });

  it('renders workspace heading without useBlocker data router error (WKS2F2-01, WKS2F2-02)', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    renderDetail('/workspace/1');

    expect(await screen.findByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument();
    expect(
      consoleError.mock.calls.some((call) => String(call[0]).includes('useBlocker must be used within a data router')),
    ).toBe(false);

    consoleError.mockRestore();
  });

  it('shows toolbar actions when workspace has widgets (WKS2F2-03)', async () => {
    renderDetail('/workspace/1');

    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar widget' })).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument();
  });

  it('shows empty state when workspace has no widgets (WKS2F2-03)', async () => {
    vi.mocked(getWorkspace).mockResolvedValue({ id: 1, nome: 'Planejamento', widgets: [] });
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Planejamento', totalWidgets: 0 }]);

    renderDetail('/workspace/1');

    expect(await screen.findByRole('status', { name: 'Workspace vazio' })).toBeInTheDocument();
  });
});
