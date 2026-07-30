import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Dashboard from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => children,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
}));

vi.mock('../../services/dashboardService', () => ({
  getDashboardStats: vi.fn().mockResolvedValue({
    totalFuncionarios: 0,
    custoMensalFolha: 0,
    totalBeneficiosAtivos: 0,
    porLinhaNegocio: [],
    porCentroCusto: [],
    porCargo: [],
    totalProventos: 0,
    totalDescontos: 0,
    topProventos: [],
    topDescontos: [],
    evolucaoMensal: [],
  }),
}));

describe('Dashboard page', () => {
  it('renders the page title after loading stats', async () => {
    renderWithProviders(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard Gerencial' })).toBeInTheDocument();
    });
  });
});
