import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import WorkspaceDetailPage from './WorkspaceDetailPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getWorkspace, listWorkspaces, listWidgetDefinitions } from '../../services/workspaceService';

vi.mock('./hooks/useUnsavedChangesGuard', () => ({
  useUnsavedChangesGuard: vi.fn(),
}));

vi.mock('../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  getWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
  listWidgetDefinitions: vi.fn().mockResolvedValue([]),
  installOrcamentoTemplate: vi.fn(),
  createWidgetDefinition: vi.fn(),
}));

vi.mock('./WorkspaceGrid', () => ({
  WorkspaceGrid: () => <div role="region" aria-label="Grid de widgets do workspace">grid</div>,
}));

function renderDetail(route = '/workspace/1') {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/:workspaceId" element={<WorkspaceDetailPage />} />
    </Routes>,
    { route },
  );
}

describe('WorkspaceDetailPage', () => {
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

  it('renders toolbar with workspace name and widget count (WKS2-08)', async () => {
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument());
    expect(screen.getByText('1 widget')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Adicionar widget' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Instalar template' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument();
  });

  it('shows editando chip and save/cancel in edit mode (WKS2-09)', async () => {
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    expect(screen.getByText('editando')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument();
  });

  it('registers unsaved changes guard when editing', async () => {
    const { useUnsavedChangesGuard } = await import('./hooks/useUnsavedChangesGuard');
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    await waitFor(() =>
      expect(vi.mocked(useUnsavedChangesGuard)).toHaveBeenCalledWith(
        expect.objectContaining({ dirty: expect.any(Boolean) }),
      ),
    );
  });

  it('shows 404 for invalid workspace id', async () => {
    renderDetail('/workspace/999');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/não encontrado/i));
  });

  it('shows empty state with shortcuts when workspace has no widgets', async () => {
    vi.mocked(getWorkspace).mockResolvedValue({ id: 1, nome: 'Planejamento', widgets: [] });
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Planejamento', totalWidgets: 0 }]);
    renderDetail('/workspace/1');
    await waitFor(() => expect(screen.getByRole('status', { name: 'Workspace vazio' })).toBeInTheDocument());
    expect(screen.getAllByRole('button', { name: 'Adicionar widget' }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByRole('button', { name: 'Instalar template' }).length).toBeGreaterThanOrEqual(1);
  });
});
