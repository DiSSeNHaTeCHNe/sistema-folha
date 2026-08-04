import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, within } from '@testing-library/react';
import { DashboardGrid } from './DashboardGrid';
import { renderWithProviders } from '../../test/renderWithProviders';
import type { DashboardStats } from '../../services/dashboardService';
import type { WidgetInstance } from './types';

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

const stats: DashboardStats = {
  totalFuncionarios: 10,
  custoMensalFolha: 1000,
  totalBeneficiosAtivos: 2,
  porLinhaNegocio: [],
  porCentroCusto: [],
  porCargo: [],
  totalProventos: 100,
  totalDescontos: 20,
  topProventos: [],
  topDescontos: [],
  evolucaoMensal: [],
};

const widgets: WidgetInstance[] = [
  { widgetId: 'kpi-total-funcionarios', instanceId: 'w1', ordem: 0, colSpan: 3, rowSpan: 1 },
  { widgetId: 'kpi-beneficios-ativos', instanceId: 'w2', ordem: 1, colSpan: 3, rowSpan: 1 },
];

describe('DashboardGrid', () => {
  it('renders widgets in ordem', () => {
    renderWithProviders(
      <DashboardGrid widgets={widgets} stats={stats} editMode={false} />,
    );
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('shows width presets in edit mode and updates colSpan', () => {
    const onWidgetsChange = vi.fn();
    renderWithProviders(
      <DashboardGrid widgets={widgets} stats={stats} editMode onWidgetsChange={onWidgetsChange} />,
    );

    const grupo = screen.getByRole('group', { name: 'Largura do widget Total de Funcionários' });
    fireEvent.click(within(grupo).getByRole('button', { name: 'Largura G' }));

    expect(onWidgetsChange).toHaveBeenCalled();
    const next = onWidgetsChange.mock.calls.at(-1)?.[0] as WidgetInstance[];
    expect(next[0].colSpan).toBe(6);
  });

  it('reorders widgets on drag end in edit mode', () => {
    const onWidgetsChange = vi.fn();
    renderWithProviders(
      <DashboardGrid widgets={widgets} stats={stats} editMode onWidgetsChange={onWidgetsChange} />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Simular reordenação por teclado' }));

    expect(onWidgetsChange).toHaveBeenCalled();
    const next = onWidgetsChange.mock.calls.at(-1)?.[0] as WidgetInstance[];
    expect(next[0].instanceId).toBe('w2');
    expect(next[1].instanceId).toBe('w1');
    expect(next[0].ordem).toBe(0);
    expect(next[1].ordem).toBe(1);
  });

  it('uses responsive span without mutating widget data', () => {
    renderWithProviders(
      <DashboardGrid widgets={widgets} stats={stats} editMode={false} />,
    );
    expect(widgets[0].colSpan).toBe(3);
    expect(screen.getByLabelText('Total de Funcionários')).toBeInTheDocument();
  });
});
