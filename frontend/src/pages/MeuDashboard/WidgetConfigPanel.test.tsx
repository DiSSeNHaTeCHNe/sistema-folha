import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';
import { WidgetConfigPanel } from './WidgetConfigPanel';
import { buildFuncionariosPorCentroPie } from './widgets/chartUtils';
import type { DashboardStats } from '../../services/dashboardService';
import { resolveTopN } from './widgetConfigOptions';
import { criarTema, TEMA_PADRAO } from '../../theme/themes';
import { useScopedFilterOptions } from './hooks/useScopedFilterOptions';

const mockScopedCentros = [
  { id: 1, descricao: 'TI', ativo: true, linhaNegocioId: 10 },
  { id: 2, descricao: 'RH', ativo: true, linhaNegocioId: 20 },
];

const mockScopedLinhas = [
  { id: 10, descricao: 'Tecnologia', ativo: true },
  { id: 20, descricao: 'Administrativo', ativo: true },
];

vi.mock('./hooks/useScopedFilterOptions', () => ({
  useScopedFilterOptions: vi.fn(() => ({
    centrosCusto: mockScopedCentros,
    linhasNegocio: mockScopedLinhas,
    loading: false,
  })),
}));

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
    vi.mocked(useScopedFilterOptions).mockReturnValue({
      centrosCusto: mockScopedCentros,
      linhasNegocio: mockScopedLinhas,
      loading: false,
    });
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

  it('updates tipoVisualizacao config for distribution widget (DASHC-34)', async () => {
    const onChange = vi.fn();
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="grafico-funcionarios-por-cc"
        config={null}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));
    fireEvent.mouseDown(screen.getByLabelText('Visualização'));
    const listbox = await screen.findByRole('listbox');
    fireEvent.click(within(listbox).getByRole('option', { name: 'Barras' }));
    expect(onChange).toHaveBeenCalledWith({ tipoVisualizacao: 'BAR' });
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

  it('updates CC/LN filter config scoped to user access (DASHC-33)', async () => {
    vi.mocked(useScopedFilterOptions).mockReturnValue({
      centrosCusto: [mockScopedCentros[1]],
      linhasNegocio: [mockScopedLinhas[1]],
      loading: false,
    });

    const onChange = vi.fn();
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="grafico-funcionarios-por-cc"
        config={null}
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));

    fireEvent.mouseDown(screen.getByLabelText('Centro de Custo'));
    const listbox = await screen.findByRole('listbox');
    expect(within(listbox).queryByRole('option', { name: 'TI' })).not.toBeInTheDocument();
    fireEvent.click(within(listbox).getByRole('option', { name: 'RH' }));
    expect(onChange).toHaveBeenCalledWith({ centroCustoId: 2 });
  });

  it('updates linhaNegocioId filter config (DASHC-33)', async () => {
    vi.mocked(useScopedFilterOptions).mockReturnValue({
      centrosCusto: [mockScopedCentros[1]],
      linhasNegocio: [mockScopedLinhas[1]],
      loading: false,
    });

    const onChange = vi.fn();
    renderWithProviders(
      <WidgetConfigPanel
        widgetId="grafico-funcionarios-por-cc"
        config={{ centroCustoId: 2 }}
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Configurações do widget' }));
    fireEvent.mouseDown(screen.getByLabelText('Linha de Negócio'));
    const listbox = await screen.findByRole('listbox');
    expect(within(listbox).queryByRole('option', { name: 'Tecnologia' })).not.toBeInTheDocument();
    fireEvent.click(within(listbox).getByRole('option', { name: 'Administrativo' }));
    expect(onChange).toHaveBeenCalledWith({ centroCustoId: 2, linhaNegocioId: 20 });
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
  const chartColors = criarTema(TEMA_PADRAO).palette.charts;

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
