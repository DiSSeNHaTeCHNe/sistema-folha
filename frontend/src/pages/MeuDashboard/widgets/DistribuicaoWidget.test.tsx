import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import { CLASSICO_CHARTS } from '../../../theme/themes';
import { DistribuicaoWidget } from './DistribuicaoWidget';
import type { PieLegendEntry } from './chartUtils';

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
  PieChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="pie-chart">{children}</div>
  ),
  Pie: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Cell: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="bar-chart">{children}</div>
  ),
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
}));

const sampleData: PieLegendEntry[] = [
  { name: 'CC A', fullName: 'Centro A', value: 10, color: CLASSICO_CHARTS[0] },
  { name: 'CC B', fullName: 'Centro B', value: 5, color: CLASSICO_CHARTS[1] },
];

describe('DistribuicaoWidget visualization switch (DASHC-34)', () => {
  it('renders PieChart by default with same data', () => {
    renderWithProviders(<DistribuicaoWidget title="Distribuição" data={sampleData} />);

    expect(screen.getByTestId('pie-chart')).toBeInTheDocument();
    expect(screen.queryByTestId('bar-chart')).not.toBeInTheDocument();
    expect(screen.getByText('CC A: 10')).toBeInTheDocument();
    expect(screen.getByText('CC B: 5')).toBeInTheDocument();
  });

  it('renders BarChart when tipoVisualizacao is BAR with same data', () => {
    renderWithProviders(
      <DistribuicaoWidget title="Distribuição" data={sampleData} tipoVisualizacao="BAR" />,
    );

    expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    expect(screen.queryByTestId('pie-chart')).not.toBeInTheDocument();
    expect(screen.getByText('CC A: 10')).toBeInTheDocument();
    expect(screen.getByText('CC B: 5')).toBeInTheDocument();
  });

  it('switches from pie to bar without changing legend data', () => {
    const { rerender } = renderWithProviders(
      <DistribuicaoWidget title="Distribuição" data={sampleData} tipoVisualizacao="PIE" />,
    );

    expect(screen.getByTestId('pie-chart')).toBeInTheDocument();

    rerender(<DistribuicaoWidget title="Distribuição" data={sampleData} tipoVisualizacao="BAR" />);

    expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    expect(screen.queryByTestId('pie-chart')).not.toBeInTheDocument();
    expect(screen.getByText('CC A: 10')).toBeInTheDocument();
  });
});
