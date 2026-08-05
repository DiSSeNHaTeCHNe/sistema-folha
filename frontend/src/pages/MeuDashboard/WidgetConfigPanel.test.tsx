import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';
import { WidgetConfigPanel } from './WidgetConfigPanel';
import { buildFuncionariosPorCentroPie } from './widgets/chartUtils';
import type { DashboardStats } from '../../services/dashboardService';
import { resolveTopN } from './widgetConfigOptions';

vi.mock('../../services/resumoFolhaPagamentoService', () => ({
  resumoFolhaPagamentoService: {
    listarMaisRecentes: vi.fn(),
  },
}));

const mockResumos = [
  {
    id: 1,
    competenciaInicio: '2026-06-01',
    competenciaFim: '2026-06-30',
    totalEmpregados: 10,
    totalEncargos: 0,
    totalPagamentos: 0,
    totalDescontos: 0,
    totalLiquido: 0,
    totalBruto: 0,
    totalCustoEmpresa: 0,
    dataImportacao: '2026-06-01',
    decimoTerceiro: false,
    ativo: true,
  },
];

describe('WidgetConfigPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(resumoFolhaPagamentoService.listarMaisRecentes).mockResolvedValue(mockResumos);
  });

  it('persists competencia override in config (DASHC-29)', async () => {
    const onChange = vi.fn();
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="kpi-total-funcionarios"
        config={null}
        onChange={onChange}
      />,
    );
    await waitFor(() => expect(resumoFolhaPagamentoService.listarMaisRecentes).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));
    fireEvent.mouseDown(screen.getByLabelText('Competência fixa'));
    const listbox = await screen.findByRole('listbox');
    fireEvent.click(within(listbox).getByRole('option', { name: 'Jun/2026' }));
    expect(onChange).toHaveBeenCalledWith({ competencia: '2026-06' });
  });

  it('updates topN config for distribution widget (DASHC-32)', async () => {
    const onChange = vi.fn();
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="grafico-custo-por-cc"
        config={null}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Quantidade de itens topN' }), {
      target: { value: '10' },
    });
    expect(onChange).toHaveBeenCalledWith({ topN: 10 });
  });

  it('shows validation errors from parent', () => {
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="grafico-custo-por-cc"
        config={{ topN: 99 }}
        onChange={() => {}}
        validationErrors={['topN deve estar entre 1 e 50']}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));
    expect(screen.getByRole('alert')).toHaveTextContent('topN deve estar entre 1 e 50');
  });
});

describe('topN rendering (DASHC-32)', () => {
  const chartColors = ['#111', '#222', '#333', '#444', '#555', '#666', '#777', '#888', '#999', '#aaa'];

  const statsWithSevenCentros: DashboardStats = {
    totalFuncionarios: 70,
    custoMensalFolha: 700000,
    totalBeneficiosAtivos: 10,
    porLinhaNegocio: [],
    porCentroCusto: Array.from({ length: 10 }, (_, index) => ({
      id: index + 1,
      descricao: `CC ${index + 1}`,
      quantidadeFuncionarios: 10 - index,
      valorTotal: (10 - index) * 1000,
    })),
    porCargo: [],
    totalProventos: 0,
    totalDescontos: 0,
    topProventos: [],
    topDescontos: [],
    evolucaoMensal: [],
  };

  it('topN=10 includes 7th centro de custo in chart data', () => {
    const topN = resolveTopN('grafico-funcionarios-por-cc', { topN: 10 });
    expect(topN).toBe(10);
    const data = buildFuncionariosPorCentroPie(statsWithSevenCentros, chartColors, topN);
    expect(data).toHaveLength(10);
    expect(data[6].fullName).toBe('CC 7');
  });

  it('default topN=5 excludes 7th centro de custo', () => {
    const topN = resolveTopN('grafico-funcionarios-por-cc', null);
    expect(topN).toBe(5);
    const data = buildFuncionariosPorCentroPie(statsWithSevenCentros, chartColors, topN);
    expect(data).toHaveLength(5);
    expect(data.find((entry) => entry.fullName === 'CC 7')).toBeUndefined();
  });
});
