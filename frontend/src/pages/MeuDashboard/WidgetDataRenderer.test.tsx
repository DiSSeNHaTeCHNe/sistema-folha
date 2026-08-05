import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { WidgetDataRenderer } from './WidgetDataRenderer';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getWidgetData } from '../../services/dashboardWidgetService';
import type { WidgetData } from './types';
import { getWidgetDefinition } from './widgets/registry';

vi.mock('../../services/dashboardWidgetService', () => ({
  buildWidgetQueryParams: vi.fn((config, competenciaGlobal) => {
    const params: Record<string, string | number> = {};
    const competencia = config?.competencia ?? competenciaGlobal ?? undefined;
    if (competencia) {
      params.competencia = competencia;
    }
    if (config?.topN != null) {
      params.topN = config.topN;
    }
    return params;
  }),
  getWidgetData: vi.fn(),
}));

const mockKpiData: WidgetData = {
  widgetId: 'kpi-total-funcionarios',
  competencia: '2026-06',
  semDados: false,
  totalFuncionarios: 42,
};

const mockEmptyData: WidgetData = {
  widgetId: 'kpi-total-funcionarios',
  competencia: '2026-01',
  semDados: true,
};

const instance = {
  widgetId: 'kpi-total-funcionarios',
  instanceId: 'inst-1',
  ordem: 0,
  colSpan: 3,
  rowSpan: 1,
};

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('WidgetDataRenderer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows explicit empty state when semDados (DASHC-44)', async () => {
    vi.mocked(getWidgetData).mockResolvedValue(mockEmptyData);
    const definition = getWidgetDefinition('kpi-total-funcionarios')!;
    renderWithProviders(
      <WidgetDataRenderer
        instance={instance}
        competenciaGlobal={null}
        editMode={false}
        definition={definition}
      />,
      { wrapper: createWrapper() },
    );
    expect(await screen.findByRole('status', { name: /Sem dados para Total de Funcionários/i })).toBeInTheDocument();
    expect(screen.queryByText('42')).not.toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('shows error state with retry action', async () => {
    vi.mocked(getWidgetData).mockRejectedValue(new Error('network'));
    const definition = getWidgetDefinition('kpi-total-funcionarios')!;
    renderWithProviders(
      <WidgetDataRenderer
        instance={instance}
        competenciaGlobal={null}
        editMode={false}
        definition={definition}
      />,
      { wrapper: createWrapper() },
    );
    expect(await screen.findByText(/Erro ao carregar dados do widget/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Recarregar' })).toBeInTheDocument();
  });

  it('renders widget content when data is available', async () => {
    vi.mocked(getWidgetData).mockResolvedValue(mockKpiData);
    const definition = getWidgetDefinition('kpi-total-funcionarios')!;
    renderWithProviders(
      <WidgetDataRenderer
        instance={instance}
        competenciaGlobal="2026-06"
        editMode={false}
        definition={definition}
      />,
      { wrapper: createWrapper() },
    );
    expect(await screen.findByText('42')).toBeInTheDocument();
  });
});

describe('useWidgetData integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getWidgetData).mockResolvedValue(mockKpiData);
  });

  it('fetches per-widget data independently', async () => {
    const { useWidgetData } = await import('./hooks/useWidgetData');
    const { result } = renderHook(
      () => useWidgetData('kpi-total-funcionarios', 'inst-1', {}, '2026-06'),
      { wrapper: createWrapper() },
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(getWidgetData).toHaveBeenCalledWith('kpi-total-funcionarios', { competencia: '2026-06' });
  });
});
