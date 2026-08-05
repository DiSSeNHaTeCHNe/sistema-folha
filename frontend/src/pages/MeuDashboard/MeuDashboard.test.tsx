import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import MeuDashboard from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getDashboardStats } from '../../services/dashboardService';
import { getWidgetData } from '../../services/dashboardWidgetService';
import {
  getDashboardLayout,
  getWidgetCatalog,
  resetDashboardLayout,
  saveDashboardLayout,
} from '../../services/dashboardLayoutService';
import type { DashboardStats } from '../../services/dashboardService';
import type { DashboardLayout, WidgetData } from './types';

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

vi.mock('../../services/dashboardService', () => ({
  getDashboardStats: vi.fn(),
}));

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

vi.mock('../../services/dashboardLayoutService', () => ({
  getDashboardLayout: vi.fn(),
  getWidgetCatalog: vi.fn(),
  saveDashboardLayout: vi.fn(),
  resetDashboardLayout: vi.fn(),
}));

const mockStats: DashboardStats = {
  totalFuncionarios: 42,
  custoMensalFolha: 125000.5,
  totalBeneficiosAtivos: 18,
  porLinhaNegocio: [{ id: 1, descricao: 'Linha Alpha', quantidadeFuncionarios: 20, valorTotal: 80000 }],
  porCentroCusto: [{ id: 1, descricao: 'Centro Admin', quantidadeFuncionarios: 15, valorTotal: 50000 }],
  porCargo: [{ id: 1, descricao: 'Analista', quantidadeFuncionarios: 5, valorMedio: 5000, valorTotal: 25000 }],
  totalProventos: 100000,
  totalDescontos: 25000,
  topProventos: [{ id: 1, codigo: '001', descricao: 'Salário', valorTotal: 80000, quantidadeOcorrencias: 40 }],
  topDescontos: [{ id: 2, codigo: '101', descricao: 'INSS', valorTotal: 15000, quantidadeOcorrencias: 40 }],
  evolucaoMensal: [{ mesAno: '2026-06', valorTotal: 125000, quantidadeFuncionarios: 42 }],
};

function toWidgetData(widgetId: string): WidgetData {
  return {
    widgetId,
    competencia: '2026-06',
    semDados: false,
    totalFuncionarios: mockStats.totalFuncionarios,
    custoMensalFolha: mockStats.custoMensalFolha,
    totalBeneficiosAtivos: mockStats.totalBeneficiosAtivos,
    totalProventos: mockStats.totalProventos,
    totalDescontos: mockStats.totalDescontos,
    porLinhaNegocio: mockStats.porLinhaNegocio,
    porCentroCusto: mockStats.porCentroCusto,
    porCargo: mockStats.porCargo,
    topProventos: mockStats.topProventos,
    topDescontos: mockStats.topDescontos,
    evolucaoMensal: mockStats.evolucaoMensal,
  };
}

const defaultLayout: DashboardLayout = {
  id: 1,
  nome: 'Meu dashboard',
  widgets: [
    { widgetId: 'kpi-total-funcionarios', instanceId: 'a1', ordem: 0, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-custo-empresa', instanceId: 'a2', ordem: 1, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-beneficios-ativos', instanceId: 'a3', ordem: 2, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-relacao-pd', instanceId: 'a4', ordem: 3, colSpan: 3, rowSpan: 1 },
    { widgetId: 'grafico-evolucao-mensal', instanceId: 'a5', ordem: 4, colSpan: 12, rowSpan: 2 },
    { widgetId: 'grafico-funcionarios-por-cc', instanceId: 'a6', ordem: 5, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-funcionarios-por-linha', instanceId: 'a7', ordem: 6, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-custo-por-cc', instanceId: 'a8', ordem: 7, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-custo-por-linha', instanceId: 'a9', ordem: 8, colSpan: 3, rowSpan: 2 },
    { widgetId: 'lista-top-proventos', instanceId: 'a10', ordem: 9, colSpan: 6, rowSpan: 2 },
    { widgetId: 'lista-top-descontos', instanceId: 'a11', ordem: 10, colSpan: 6, rowSpan: 2 },
  ],
};

describe('MeuDashboard shell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getDashboardLayout).mockResolvedValue(defaultLayout);
    vi.mocked(getWidgetCatalog).mockResolvedValue([]);
    vi.mocked(getWidgetData).mockImplementation(async (widgetId) => toWidgetData(widgetId));
    vi.mocked(saveDashboardLayout).mockImplementation(async (layout) => layout);
    vi.mocked(resetDashboardLayout).mockResolvedValue(undefined);
  });

  it('does not call getDashboardStats (DASHC-40)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Meu Dashboard' })).toBeInTheDocument();
    });
    expect(getDashboardStats).not.toHaveBeenCalled();
  });

  it('renders page title and default layout widgets', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Meu Dashboard' })).toBeInTheDocument();
    });
    expect(await screen.findByText('42')).toBeInTheDocument();
    expect(screen.getByText('Evolução da Folha de Pagamento')).toBeInTheDocument();
    expect(screen.getByText('001 - Salário')).toBeInTheDocument();
  });

  it('shows edit toolbar actions in edit mode', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Salvar' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Restaurar padrão' })).toBeInTheDocument();
  });

  it('renders 11 default widgets with per-widget data (DASHC-01, DASHC-02, DASHC-40)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
    expect(defaultLayout.widgets).toHaveLength(11);
    expect(screen.getByLabelText('Total de Funcionários')).toBeInTheDocument();
    expect(screen.getByLabelText('Custo Empresa')).toBeInTheDocument();
    expect(screen.getByLabelText('Benefícios Ativos')).toBeInTheDocument();
    expect(screen.getByLabelText('Relação P/D')).toBeInTheDocument();
    expect(screen.getByText('R$ 125.000,50')).toBeInTheDocument();
    expect(screen.getByText('80.0%')).toBeInTheDocument();
    expect(getWidgetData).toHaveBeenCalled();
  });

  it('shows explicit empty state when competência has no data (DASHC-30)', async () => {
    vi.mocked(getWidgetData).mockResolvedValue({
      widgetId: 'kpi-total-funcionarios',
      competencia: '2026-01',
      semDados: true,
    });
    renderWithProviders(<MeuDashboard />);
    expect(
      await screen.findByRole('status', { name: /Sem dados para Total de Funcionários/i }),
    ).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('hides drag, resize and remove controls outside edit mode (DASHC-11)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /Reordenar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Remover/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('group', { name: /Largura do widget/i })).not.toBeInTheDocument();
  });

  it('removes widget in edit mode (DASHC-10)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('001 - Salário')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Remover Top Proventos' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Remover Top Proventos' }));
    expect(screen.queryByText('001 - Salário')).not.toBeInTheDocument();
  });

  it('cancel discards draft and restores saved layout (DASHC-21)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('001 - Salário')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Remover Top Proventos' }));
    expect(screen.queryByText('001 - Salário')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.getByText('001 - Salário')).toBeInTheDocument());
    expect(saveDashboardLayout).not.toHaveBeenCalled();
  });

  it('save persists layout changes (DASHC-19, DASHC-20)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('001 - Salário')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Remover Top Proventos' }));
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(saveDashboardLayout).toHaveBeenCalled());
    expect(screen.queryByText('001 - Salário')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument();
  });

  it('restore default after confirmation (DASHC-22)', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Editar layout' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar layout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Restaurar padrão' }));
    fireEvent.click(screen.getByRole('button', { name: 'Restaurar' }));
    await waitFor(() => expect(resetDashboardLayout).toHaveBeenCalled());
  });

  it('ignores unknown widget ids from saved layout (DASHC-27)', async () => {
    vi.mocked(getDashboardLayout).mockResolvedValue({
      ...defaultLayout,
      widgets: [
        ...defaultLayout.widgets,
        { widgetId: 'widget-removido-do-catalogo', instanceId: 'ghost', ordem: 99, colSpan: 3, rowSpan: 1 },
      ],
    });
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
    expect(screen.queryByLabelText('widget-removido-do-catalogo')).not.toBeInTheDocument();
    expect(screen.getAllByLabelText(/Total de Funcionários|Custo Empresa|Benefícios Ativos|Relação P\/D/i)).toHaveLength(4);
  });
});
