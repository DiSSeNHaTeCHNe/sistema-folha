import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import type { DashboardStats } from '../../../services/dashboardService';
import { renderWithProviders } from '../../../test/renderWithProviders';
import type { WidgetInstance } from '../types';
import { WIDGET_REGISTRY } from './registry';

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

const mockStats: DashboardStats = {
  totalFuncionarios: 42,
  custoMensalFolha: 125000.5,
  totalBeneficiosAtivos: 18,
  porLinhaNegocio: [
    { id: 1, descricao: 'Linha Alpha', quantidadeFuncionarios: 20, valorTotal: 80000 },
  ],
  porCentroCusto: [
    { id: 1, descricao: 'Centro Admin', quantidadeFuncionarios: 15, valorTotal: 50000 },
  ],
  porCargo: [{ id: 1, descricao: 'Analista', quantidadeFuncionarios: 5, valorMedio: 5000, valorTotal: 25000 }],
  totalProventos: 100000,
  totalDescontos: 25000,
  topProventos: [{ id: 1, codigo: '001', descricao: 'Salário', valorTotal: 80000, quantidadeOcorrencias: 40 }],
  topDescontos: [{ id: 2, codigo: '101', descricao: 'INSS', valorTotal: 15000, quantidadeOcorrencias: 40 }],
  evolucaoMensal: [{ mesAno: '2026-06', valorTotal: 125000, quantidadeFuncionarios: 42 }],
};

const instance: WidgetInstance = {
  widgetId: 'placeholder',
  instanceId: 'abc12345',
  ordem: 0,
  colSpan: 3,
  rowSpan: 1,
};

const expectedContent: Record<string, string> = {
  'kpi-total-funcionarios': '42',
  'kpi-custo-empresa': 'R$ 125.000,50',
  'kpi-beneficios-ativos': '18',
  'kpi-relacao-pd': '80.0%',
  'grafico-evolucao-mensal': 'Evolução da Folha de Pagamento',
  'grafico-funcionarios-por-cc': 'Funcionários por Centro de Custo',
  'grafico-funcionarios-por-linha': 'Funcionários por Linha de Negócio',
  'grafico-custo-por-cc': 'Custo Folha por Centro de Custo',
  'grafico-custo-por-linha': 'Custo Folha por Linha de Negócio',
  'lista-top-proventos': '001 - Salário',
  'lista-top-descontos': '101 - INSS',
  'grafico-funcionarios-por-cargo': 'Funcionários por Cargo',
};

describe('widget registry', () => {
  it('exports 12 widget definitions', () => {
    expect(WIDGET_REGISTRY).toHaveLength(12);
  });

  it.each(WIDGET_REGISTRY.map((w) => [w.id, w.titulo, w.Component]))(
    'renders %s without variation chips',
    (widgetId, _titulo, Component) => {
      renderWithProviders(
        <Component instance={{ ...instance, widgetId }} stats={mockStats} editMode={false} />,
      );
      expect(screen.getByText(expectedContent[widgetId])).toBeInTheDocument();
      expect(screen.queryByText(/\+\d+\.\d+% este mês/)).not.toBeInTheDocument();
      expect(screen.queryByText('Estável')).not.toBeInTheDocument();
    },
  );
});
