import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { DashboardGrid } from './DashboardGrid';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getWidgetData } from '../../services/dashboardWidgetService';
import type { WidgetData, WidgetInstance } from './types';

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

vi.mock('@dnd-kit/core', async () => {
  const actual = await vi.importActual<typeof import('@dnd-kit/core')>('@dnd-kit/core');
  return {
    ...actual,
    DndContext: ({
      children,
      onDragEnd,
    }: {
      children: React.ReactNode;
      onDragEnd: (event: { active: { id: string }; over: { id: string } }) => void;
    }) => (
      <div>
        {children}
        <button
          type="button"
          onClick={() => onDragEnd({ active: { id: 'w1' }, over: { id: 'w2' } })}
        >
          Simular reordenação por teclado
        </button>
      </div>
    ),
  };
});

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Cell: () => null,
}));

const widgets: WidgetInstance[] = [
  { widgetId: 'kpi-total-funcionarios', instanceId: 'w1', ordem: 0, colSpan: 3, rowSpan: 1 },
  { widgetId: 'kpi-beneficios-ativos', instanceId: 'w2', ordem: 1, colSpan: 3, rowSpan: 1 },
];

function mockWidgetResponse(widgetId: string): WidgetData {
  if (widgetId === 'kpi-total-funcionarios') {
    return {
      widgetId,
      competencia: '2026-06',
      semDados: false,
      totalFuncionarios: 10,
    };
  }
  return {
    widgetId,
    competencia: '2026-06',
    semDados: false,
    totalBeneficiosAtivos: 2,
  };
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('DashboardGrid', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getWidgetData).mockImplementation(async (widgetId) => mockWidgetResponse(widgetId));
  });

  it('renders widgets in ordem', async () => {
    renderWithProviders(
      <DashboardGrid widgets={widgets} competenciaGlobal="2026-06" editMode={false} />,
      { wrapper: createWrapper() },
    );
    expect(await screen.findByText('10')).toBeInTheDocument();
    expect(await screen.findByText('2')).toBeInTheDocument();
  });

  it('shows width presets in edit mode and updates colSpan', async () => {
    const onWidgetsChange = vi.fn();
    renderWithProviders(
      <DashboardGrid
        widgets={widgets}
        competenciaGlobal="2026-06"
        editMode
        onWidgetsChange={onWidgetsChange}
      />,
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(screen.getByText('10')).toBeInTheDocument());

    const grupo = screen.getByRole('group', { name: 'Largura do widget Total de Funcionários' });
    fireEvent.click(within(grupo).getByRole('button', { name: 'Largura G' }));

    expect(onWidgetsChange).toHaveBeenCalled();
    const next = onWidgetsChange.mock.calls.at(-1)?.[0] as WidgetInstance[];
    expect(next[0].colSpan).toBe(6);
  });

  it('reorders widgets on drag end in edit mode', async () => {
    const onWidgetsChange = vi.fn();
    renderWithProviders(
      <DashboardGrid
        widgets={widgets}
        competenciaGlobal="2026-06"
        editMode
        onWidgetsChange={onWidgetsChange}
      />,
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(screen.getByText('10')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Simular reordenação por teclado' }));

    expect(onWidgetsChange).toHaveBeenCalled();
    const next = onWidgetsChange.mock.calls.at(-1)?.[0] as WidgetInstance[];
    expect(next[0].instanceId).toBe('w2');
    expect(next[1].instanceId).toBe('w1');
    expect(next[0].ordem).toBe(0);
    expect(next[1].ordem).toBe(1);
  });

  it('uses responsive span without mutating widget data', async () => {
    renderWithProviders(
      <DashboardGrid widgets={widgets} competenciaGlobal="2026-06" editMode={false} />,
      { wrapper: createWrapper() },
    );
    await waitFor(() => expect(screen.getByLabelText('Total de Funcionários')).toBeInTheDocument());
    expect(widgets[0].colSpan).toBe(3);
  });
});
