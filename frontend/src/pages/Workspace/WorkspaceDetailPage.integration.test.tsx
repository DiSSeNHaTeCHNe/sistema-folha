import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { useNavigate } from 'react-router-dom';
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
  WorkspaceGrid: ({
    editMode,
    onRemoveWidget,
  }: {
    editMode: boolean;
    onRemoveWidget?: (instanceId: string) => void;
  }) =>
    editMode && onRemoveWidget ? (
      <div role="region" aria-label="Grid de widgets do workspace">
        <button type="button" onClick={() => onRemoveWidget('w1')}>
          Remover widget de teste
        </button>
      </div>
    ) : (
      <div role="region" aria-label="Grid de widgets do workspace">
        grid
      </div>
    ),
}));

function WorkspaceDetailWithNav() {
  const navigate = useNavigate();
  return (
    <>
      <button type="button" onClick={() => navigate('/workspace')}>
        Ir para hub
      </button>
      <WorkspaceDetailPage />
    </>
  );
}

const detailRoutes = [
  { path: '/workspace/:workspaceId', element: <WorkspaceDetailWithNav /> },
  { path: '/workspace', element: <h1>Hub de destino</h1> },
];

function renderDetail(route = '/workspace/1') {
  return renderWithDataRouter(<WorkspaceDetailPage />, {
    routes: detailRoutes,
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

describe('WorkspaceDetailPage unsaved changes guard integration (WKS2F2-09, WKS2F2-10, WKS2F2-11)', () => {
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

  async function enterDirtyEditMode() {
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    await waitFor(() => expect(screen.getByText('editando')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Remover widget de teste' }));
  }

  it('prompts confirm before in-app navigation when edit mode is dirty (WKS2F2-09)', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    await enterDirtyEditMode();

    fireEvent.click(screen.getByRole('button', { name: 'Ir para hub' }));

    expect(confirmSpy).toHaveBeenCalledWith('Existem alterações não salvas. Deseja sair sem salvar?');
  });

  it('stays on detail page when user cancels confirm (WKS2F2-10)', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await enterDirtyEditMode();

    fireEvent.click(screen.getByRole('button', { name: 'Ir para hub' }));

    expect(screen.getByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Hub de destino', level: 1 })).not.toBeInTheDocument();
  });

  it('navigates away when user confirms discard (WKS2F2-09)', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await enterDirtyEditMode();

    fireEvent.click(screen.getByRole('button', { name: 'Ir para hub' }));

    expect(await screen.findByRole('heading', { name: 'Hub de destino', level: 1 })).toBeInTheDocument();
  });

  it('navigates without confirm when layout is not dirty (WKS2F2-11)', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm');
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Ir para hub' }));

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(await screen.findByRole('heading', { name: 'Hub de destino', level: 1 })).toBeInTheDocument();
  });
});
