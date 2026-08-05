import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useWorkspaceLayout } from './useWorkspaceLayout';
import {
  createWorkspace,
  deleteWorkspace,
  getWorkspace,
  listWorkspaces,
  saveWorkspaceLayout,
} from '../../../services/workspaceService';
import type { Workspace } from '../types';

vi.mock('../../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  getWorkspace: vi.fn(),
  createWorkspace: vi.fn(),
  deleteWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
}));

const ws1: Workspace = {
  id: 1,
  nome: 'Planejamento',
  widgets: [{ instanceId: 'a1', ordem: 0, colSpan: 4, rowSpan: 1, widgetId: 'kpi-total-funcionarios' }],
};

const ws2: Workspace = {
  id: 2,
  nome: 'Trimestral',
  widgets: [{ instanceId: 'b1', ordem: 0, colSpan: 6, rowSpan: 1, userWidgetDefinitionId: 5 }],
};

describe('useWorkspaceLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listWorkspaces).mockResolvedValue([
      { id: 1, nome: 'Planejamento', totalWidgets: 1 },
      { id: 2, nome: 'Trimestral', totalWidgets: 1 },
    ]);
    vi.mocked(getWorkspace).mockImplementation(async (id) => (id === 1 ? ws1 : ws2));
    vi.mocked(createWorkspace).mockResolvedValue({ id: 3, nome: 'Novo', widgets: [] });
    vi.mocked(deleteWorkspace).mockResolvedValue(undefined);
    vi.mocked(saveWorkspaceLayout).mockImplementation(async (id, widgets) => ({
      id,
      nome: id === 1 ? 'Planejamento' : 'Trimestral',
      widgets,
    }));
  });

  it('loads summaries and selects first workspace', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.summaries).toHaveLength(2);
    expect(result.current.activeWorkspaceId).toBe(1);
  });

  it('loads layout for active workspace', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout?.widgets).toHaveLength(1));
    expect(result.current.savedLayout?.widgets[0].instanceId).toBe('a1');
  });

  it('switchWorkspace preserves per-workspace layouts', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout?.id).toBe(1));

    act(() => {
      result.current.switchWorkspace(2);
    });
    await waitFor(() => expect(result.current.savedLayout?.id).toBe(2));
    expect(result.current.savedLayout?.widgets[0].instanceId).toBe('b1');

    act(() => {
      result.current.switchWorkspace(1);
    });
    await waitFor(() => expect(result.current.savedLayout?.widgets[0].instanceId).toBe('a1'));
  });

  it('createNewWorkspace adds workspace and selects it', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.createNewWorkspace('Novo');
    });

    expect(createWorkspace).toHaveBeenCalledWith('Novo');
    expect(result.current.activeWorkspaceId).toBe(3);
    expect(result.current.summaries.some((item) => item.nome === 'Novo')).toBe(true);
  });

  it('removeWorkspace deletes and switches to remaining', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.removeWorkspace(1);
    });

    expect(deleteWorkspace).toHaveBeenCalledWith(1);
    expect(result.current.summaries).toHaveLength(1);
    expect(result.current.activeWorkspaceId).toBe(2);
  });

  it('enterEditMode creates draft clone', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout).not.toBeNull());

    act(() => {
      result.current.enterEditMode();
    });

    expect(result.current.editMode).toBe(true);
    expect(result.current.draftLayout).not.toBe(result.current.savedLayout);
    expect(result.current.draftLayout?.widgets).toEqual(result.current.savedLayout?.widgets);
  });

  it('save persists layout and exits edit mode', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout).not.toBeNull());

    act(() => {
      result.current.enterEditMode();
    });

    const updatedWidgets = [{ instanceId: 'x1', ordem: 0, colSpan: 3, rowSpan: 1, widgetId: 'kpi-custo-empresa' }];
    act(() => {
      result.current.updateDraftWidgets(updatedWidgets);
    });

    let saved = false;
    await act(async () => {
      saved = await result.current.save();
    });

    expect(saved).toBe(true);
    expect(saveWorkspaceLayout).toHaveBeenCalledWith(1, updatedWidgets);
    expect(result.current.editMode).toBe(false);
    expect(result.current.savedLayout?.widgets).toEqual(updatedWidgets);
  });

  it('cancelEdit discards draft', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout).not.toBeNull());

    act(() => {
      result.current.enterEditMode();
      result.current.updateDraftWidgets([]);
    });
    act(() => {
      result.current.cancelEdit();
    });

    expect(result.current.editMode).toBe(false);
    expect(result.current.draftLayout).toBeNull();
    expect(result.current.savedLayout?.widgets).toHaveLength(1);
  });

  it('keeps draft when save fails', async () => {
    vi.mocked(saveWorkspaceLayout).mockRejectedValue(new Error('fail'));
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout).not.toBeNull());

    act(() => {
      result.current.enterEditMode();
      result.current.updateDraftWidgets([]);
    });

    await act(async () => {
      await result.current.save();
    });

    expect(result.current.editMode).toBe(true);
    expect(result.current.draftLayout?.widgets).toHaveLength(0);
  });

  it('dirty is true when draft widgets differ from saved', async () => {
    const { result } = renderHook(() => useWorkspaceLayout());
    await waitFor(() => expect(result.current.savedLayout).not.toBeNull());

    act(() => {
      result.current.enterEditMode();
    });
    expect(result.current.dirty).toBe(false);

    act(() => {
      result.current.updateDraftWidgets([]);
    });
    expect(result.current.dirty).toBe(true);
  });
});
