import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useDashboardLayout } from './useDashboardLayout';
import {
  getDashboardLayout,
  getWidgetCatalog,
  resetDashboardLayout,
  saveDashboardLayout,
} from '../../../services/dashboardLayoutService';
import type { DashboardLayout } from '../types';
import { gravarLayoutCache, limparLayoutCache } from '../storage';

vi.mock('../../../services/dashboardLayoutService', () => ({
  getDashboardLayout: vi.fn(),
  getWidgetCatalog: vi.fn(),
  saveDashboardLayout: vi.fn(),
  resetDashboardLayout: vi.fn(),
}));

const layoutBase: DashboardLayout = {
  id: 1,
  nome: 'Meu dashboard',
  widgets: [
    { widgetId: 'kpi-total-funcionarios', instanceId: 'a1', ordem: 0, colSpan: 3, rowSpan: 1 },
    { widgetId: 'widget-desconhecido', instanceId: 'x1', ordem: 1, colSpan: 3, rowSpan: 1 },
  ],
};

const catalogo = [
  {
    widgetId: 'kpi-total-funcionarios',
    titulo: 'Total de Funcionários',
    descricao: 'KPI',
    categoria: 'KPI' as const,
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
  },
];

describe('useDashboardLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    limparLayoutCache();
    vi.mocked(getDashboardLayout).mockResolvedValue(layoutBase);
    vi.mocked(getWidgetCatalog).mockResolvedValue(catalogo);
    vi.mocked(saveDashboardLayout).mockImplementation(async (layout) => layout);
    vi.mocked(resetDashboardLayout).mockResolvedValue(undefined);
  });

  it('loads layout and filters unknown widget ids', async () => {
    const { result } = renderHook(() => useDashboardLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.savedLayout?.widgets).toHaveLength(1);
    expect(result.current.savedLayout?.widgets[0].widgetId).toBe('kpi-total-funcionarios');
  });

  it('save calls PUT and exits edit mode on success', async () => {
    const { result } = renderHook(() => useDashboardLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.enterEditMode();
    });

    const draft = result.current.draftLayout!;
    const updated = {
      ...draft,
      widgets: draft.widgets.filter((w) => w.widgetId !== 'kpi-total-funcionarios'),
    };

    act(() => {
      result.current.setDraftLayout(updated);
    });

    let saved = false;
    await act(async () => {
      saved = await result.current.save();
    });

    expect(saved).toBe(true);
    expect(saveDashboardLayout).toHaveBeenCalled();
    expect(result.current.editMode).toBe(false);
    expect(result.current.savedLayout?.widgets).toHaveLength(0);
  });

  it('keeps draft when save fails', async () => {
    vi.mocked(saveDashboardLayout).mockRejectedValue(new Error('fail'));
    const { result } = renderHook(() => useDashboardLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.enterEditMode();
    });

    await act(async () => {
      await result.current.save();
    });

    expect(result.current.editMode).toBe(true);
    expect(result.current.draftLayout).not.toBeNull();
    expect(result.current.error).toBe('Erro ao salvar layout');
  });

  it('cancel discards draft', async () => {
    const { result } = renderHook(() => useDashboardLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));

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

  it('persists layout to localStorage cache', async () => {
    const { result } = renderHook(() => useDashboardLayout());
    await waitFor(() => expect(result.current.loading).toBe(false));
    gravarLayoutCache(result.current.savedLayout!);
    expect(localStorage.getItem('sistema-folha:meu-dashboard-layout')).toContain('kpi-total-funcionarios');
  });
});
