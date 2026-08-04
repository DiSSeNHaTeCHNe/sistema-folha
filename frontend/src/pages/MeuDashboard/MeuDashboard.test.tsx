import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import MeuDashboard from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getDashboardStats } from '../../services/dashboardService';
import { getDashboardLayout } from '../../services/dashboardLayoutService';
import type { DashboardStats } from '../../services/dashboardService';
import type { DashboardLayout } from './types';

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

vi.mock('../../services/dashboardLayoutService', () => ({
  getDashboardLayout: vi.fn(),
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
    vi.mocked(getDashboardStats).mockResolvedValue(mockStats);
  });

  it('renders page title and default layout widgets', async () => {
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Meu Dashboard' })).toBeInTheDocument();
    });
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Evolução da Folha de Pagamento')).toBeInTheDocument();
    expect(screen.getByText('001 - Salário')).toBeInTheDocument();
  });

  it('shows error when layout load fails', async () => {
    vi.mocked(getDashboardLayout).mockRejectedValue(new Error('fail'));
    renderWithProviders(<MeuDashboard />);
    await waitFor(() => {
      expect(screen.getByText('Erro ao carregar Meu Dashboard')).toBeInTheDocument();
    });
  });
});
