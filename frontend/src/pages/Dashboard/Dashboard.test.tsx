import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Dashboard from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';

const showNotification = vi.fn();
const hideNotification = vi.fn();

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="chart">{children}</div>,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  XAxis: () => null,
  YAxis: ({ tickFormatter }: { tickFormatter?: (value: number) => string }) => {
    tickFormatter?.(1500);
    tickFormatter?.(500);
    return null;
  },
  Tooltip: ({ formatter }: { formatter?: (value: unknown, name: string) => unknown[] }) => {
    formatter?.(1500, 'folha');
    formatter?.(50, 'funcionarios');
    formatter?.(2000, 'custo');
    return null;
  },
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Cell: () => null,
}));

vi.mock('../../services/dashboardService', () => ({
  getDashboardStats: vi.fn(),
}));

vi.mock('../../hooks/useNotification', () => ({
  useNotification: () => ({
    notification: { open: false, message: '', severity: 'info' },
    showNotification,
    hideNotification,
  }),
}));

const fullStats: DashboardStats = {
  totalFuncionarios: 42,
  custoMensalFolha: 125000.5,
  totalBeneficiosAtivos: 18,
  porLinhaNegocio: [
    { id: 1, descricao: 'Linha de Negócio Extensa Alpha', quantidadeFuncionarios: 20, valorTotal: 80000 },
    { id: 2, descricao: 'Beta', quantidadeFuncionarios: 10, valorTotal: 30000 },
  ],
  porCentroCusto: [
    { id: 1, descricao: 'Centro de Custo Administrativo Geral', quantidadeFuncionarios: 15, valorTotal: 50000 },
    { id: 2, descricao: 'Operações', quantidadeFuncionarios: 8, valorTotal: 25000 },
  ],
  porCargo: [{ id: 1, descricao: 'Analista', quantidadeFuncionarios: 5, valorMedio: 5000, valorTotal: 25000 }],
  totalProventos: 100000,
  totalDescontos: 25000,
  topProventos: [
    { id: 1, codigo: '001', descricao: 'Salário', valorTotal: 80000, quantidadeOcorrencias: 40 },
  ],
  topDescontos: [
    { id: 2, codigo: '101', descricao: 'INSS', valorTotal: 15000, quantidadeOcorrencias: 40 },
  ],
  evolucaoMensal: [
    { mesAno: '2026-05', valorTotal: 120000, quantidadeFuncionarios: 40 },
    { mesAno: '2026-06', valorTotal: 125000, quantidadeFuncionarios: 42 },
  ],
};

function renderDashboard(state?: object) {
  return renderWithProviders(<Dashboard />, {
    route: '/dashboard',
    routerProps: state
      ? { initialEntries: [{ pathname: '/dashboard', state }] }
      : { initialEntries: ['/dashboard'] },
  });
}

describe('Dashboard page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getDashboardStats).mockResolvedValue(fullStats);
  });

  it('renders the page title after loading stats', async () => {
    renderDashboard();
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard Gerencial' })).toBeInTheDocument();
    });
  });

  it('renders main KPI cards and chart sections', async () => {
    renderDashboard();
    await waitFor(() => expect(screen.getByText('Total de Funcionários')).toBeInTheDocument());
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Evolução da Folha de Pagamento')).toBeInTheDocument();
    expect(screen.getByText('Funcionários por Centro de Custo')).toBeInTheDocument();
    expect(screen.getByText('Top 5 Proventos')).toBeInTheDocument();
    expect(screen.getByText('001 - Salário')).toBeInTheDocument();
  });

  it('shows error alert when load fails', async () => {
    vi.mocked(getDashboardStats).mockRejectedValue(new Error('fail'));
    renderDashboard();
    await waitFor(() => expect(screen.getByText('Erro ao carregar dados do dashboard')).toBeInTheDocument());
  });

  it('shows empty data message when stats is null', async () => {
    vi.mocked(getDashboardStats).mockResolvedValue(null as unknown as DashboardStats);
    renderDashboard();
    await waitFor(() => expect(screen.getByText('Nenhum dado disponível')).toBeInTheDocument());
  });

  it('shows empty evolution chart message', async () => {
    vi.mocked(getDashboardStats).mockResolvedValue({ ...fullStats, evolucaoMensal: [] });
    renderDashboard();
    await waitFor(() =>
      expect(screen.getByText('Nenhuma folha regular encontrada nos últimos 12 meses.')).toBeInTheDocument(),
    );
  });

  it('shows access denied notification from navigation state', async () => {
    renderDashboard({ acessoNegado: true });
    await waitFor(() =>
      expect(showNotification).toHaveBeenCalledWith('Acesso negado. Apenas administradores.', 'warning'),
    );
  });
});
