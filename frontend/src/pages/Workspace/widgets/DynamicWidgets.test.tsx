import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { DynamicKpiWidget } from './DynamicKpiWidget';
import { DynamicTableWidget } from './DynamicTableWidget';
import { DynamicChartWidget } from './DynamicChartWidget';
import { renderWithProviders } from '../../../test/renderWithProviders';
import type { WorkspaceWidgetData } from '../types';

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  Line: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

const baseData: WorkspaceWidgetData = {
  instanceId: 'w1',
  userWidgetDefinitionId: 1,
  widgetId: null,
  tipo: 'KPI',
  semDados: false,
  invalido: false,
  competencia: '2026-06',
  valores: {},
  linhas: [],
};

describe('DynamicKpiWidget', () => {
  it('formats monetary KPI value in pt-BR', () => {
    renderWithProviders(
      <DynamicKpiWidget title="Total" data={{ ...baseData, valores: { total: '1234.56' } }} />,
    );
    expect(screen.getByText('R$ 1.234,56')).toBeInTheDocument();
  });

  it('shows plain numeric KPI value', () => {
    renderWithProviders(
      <DynamicKpiWidget title="Qtd" data={{ ...baseData, valores: { total: '42' } }} />,
    );
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('shows dash when no values', () => {
    renderWithProviders(<DynamicKpiWidget title="Vazio" data={{ ...baseData, valores: {} }} />);
    expect(screen.getByText('-')).toBeInTheDocument();
  });
});

describe('DynamicTableWidget', () => {
  it('renders table headers from row keys', () => {
    renderWithProviders(
      <DynamicTableWidget
        title="Orçamento"
        data={{
          ...baseData,
          linhas: [{ centro: 'TI', valor: '5000.00' }],
        }}
      />,
    );
    expect(screen.getByRole('columnheader', { name: 'centro' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'valor' })).toBeInTheDocument();
  });

  it('formats money cells in pt-BR', () => {
    renderWithProviders(
      <DynamicTableWidget
        title="Orçamento"
        data={{ ...baseData, linhas: [{ valor: '2500.75' }] }}
      />,
    );
    expect(screen.getByText('R$ 2.500,75')).toBeInTheDocument();
  });

  it('shows empty status without rows', () => {
    renderWithProviders(<DynamicTableWidget title="Vazio" data={{ ...baseData, linhas: [] }} />);
    expect(screen.getByRole('status')).toHaveTextContent(/Sem linhas/i);
  });
});

describe('DynamicChartWidget', () => {
  const chartRows = [
    { label: 'Jan', valor: '1000.00' },
    { label: 'Fev', valor: '2000.50' },
  ];

  it('renders bar chart container by role', () => {
    renderWithProviders(
      <DynamicChartWidget title="Barras" data={{ ...baseData, linhas: chartRows }} variant="GRAFICO_BARRA" />,
    );
    expect(screen.getByLabelText('Gráfico Barras')).toBeInTheDocument();
  });

  it('renders line chart container by role', () => {
    renderWithProviders(
      <DynamicChartWidget title="Linha" data={{ ...baseData, linhas: chartRows }} variant="GRAFICO_LINHA" />,
    );
    expect(screen.getByLabelText('Gráfico Linha')).toBeInTheDocument();
  });

  it('shows empty status without chart rows', () => {
    renderWithProviders(
      <DynamicChartWidget title="Vazio" data={{ ...baseData, linhas: [] }} variant="GRAFICO_LINHA" />,
    );
    expect(screen.getByRole('status')).toHaveTextContent(/Sem dados para gráfico/i);
  });
});
