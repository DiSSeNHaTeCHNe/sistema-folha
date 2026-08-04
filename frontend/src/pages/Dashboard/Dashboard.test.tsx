import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Dashboard from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';
import { createTheme } from '@mui/material/styles';
import { criarTema, TEMA_IDS, TEMA_PADRAO, type TemaId } from '../../theme/themes';

const cellFills: string[] = [];

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
  Cell: ({ fill }: { fill?: string }) => {
    if (fill) {
      cellFills.push(fill);
    }
    return null;
  },
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

function renderDashboard(state?: object, temaId?: TemaId) {
  return renderWithProviders(<Dashboard />, {
    route: '/dashboard',
    temaId,
    routerProps: state
      ? { initialEntries: [{ pathname: '/dashboard', state }] }
      : { initialEntries: ['/dashboard'] },
  });
}

/**
 * Canais R/G/B de uma cor, aceitando tanto a forma hexadecimal dos tokens quanto a
 * forma funcional devolvida por getComputedStyle. Sem literal de cor no fonte,
 * para não violar a guarda de src/theme/noColorLiterals.test.ts.
 */
function canaisDaCor(cor: string): number[] {
  if (cor.startsWith('#')) {
    const hex = cor.slice(1);
    return [0, 2, 4].map((i) => parseInt(hex.slice(i, i + 2), 16));
  }
  return (cor.match(/\d+(?:\.\d+)?/g) ?? []).slice(0, 3).map(Number);
}

/** O avatar é o último filho do cabeçalho do card, ao lado do bloco de textos. */
function avatarDoCardKpi(rotulo: string): HTMLElement {
  const cabecalho = screen.getByText(rotulo).parentElement?.parentElement;
  const avatar = cabecalho?.lastElementChild;
  if (!(avatar instanceof HTMLElement)) {
    throw new Error(`Avatar do card "${rotulo}" não encontrado`);
  }
  return avatar;
}

const AVATARES_KPI = [
  { rotulo: 'Total de Funcionários', papel: 'info', tomDoIcone: 'main' },
  { rotulo: 'Custo Empresa', papel: 'success', tomDoIcone: 'main' },
  { rotulo: 'Benefícios Ativos', papel: 'warning', tomDoIcone: 'main' },
  { rotulo: 'Relação P/D', papel: 'info', tomDoIcone: 'dark' },
] as const;

describe('Dashboard page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    cellFills.length = 0;
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

  it('uses theme.palette.charts for pie chart segment colors', async () => {
    const chartPalette = criarTema(TEMA_PADRAO).palette.charts;
    renderDashboard();
    await waitFor(() => expect(screen.getByText('42')).toBeInTheDocument());
    expect(cellFills.length).toBeGreaterThan(0);
    for (const fill of cellFills) {
      expect(chartPalette).toContain(fill);
    }
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

  it.each(TEMA_IDS)('avatares de KPI derivam do tema %s', async (temaId) => {
    const palette = criarTema(temaId).palette;
    const paletteDeFabrica = createTheme().palette;
    renderDashboard(undefined, temaId);
    await waitFor(() => expect(screen.getByText('Total de Funcionários')).toBeInTheDocument());

    for (const { rotulo, papel, tomDoIcone } of AVATARES_KPI) {
      const estilo = getComputedStyle(avatarDoCardKpi(rotulo));
      expect(canaisDaCor(estilo.backgroundColor), `${temaId}: ${rotulo} fundo`).toEqual(
        canaisDaCor(palette[papel].light),
      );
      expect(canaisDaCor(estilo.color), `${temaId}: ${rotulo} ícone`).toEqual(
        canaisDaCor(palette[papel][tomDoIcone]),
      );
      expect(canaisDaCor(estilo.backgroundColor), `${temaId}: ${rotulo} fundo de fábrica`).not.toEqual(
        canaisDaCor(paletteDeFabrica[papel].light),
      );
    }
  });

  /**
   * TEMAF-10 / P1-Props AC4: o título de página não declara cor própria — herda
   * `text.primary` do documento. O jsdom não resolve a herança vinda do CssBaseline,
   * então o que se asserta aqui é o equivalente verificável: o título não pinta a cor
   * de acento e sua cor computada é a mesma do contêiner (herança). Reintroduzir
   * `color="primary"` quebra as duas asserções. A medição do valor absoluto de
   * `text.primary` é feita no navegador (T10).
   */
  it.each(TEMA_IDS)('título da página herda a cor do texto no tema %s', async (temaId) => {
    const palette = criarTema(temaId).palette;
    renderDashboard(undefined, temaId);
    const titulo = await screen.findByRole('heading', { name: 'Dashboard Gerencial' });
    const conteiner = titulo.parentElement as HTMLElement;

    expect(canaisDaCor(getComputedStyle(titulo).color), `${temaId}: título não usa acento`).not.toEqual(
      canaisDaCor(palette.primary.main),
    );
    expect(getComputedStyle(titulo).color, `${temaId}: título herda a cor do contêiner`).toBe(
      getComputedStyle(conteiner).color,
    );
  });

  // TEMAF-12 / P1-Props AC5: props color semânticas sobrevivem à remoção das props de estilo.
  it('valor de KPI com prop semântica mantém a cor success.main do tema', async () => {
    const palette = criarTema(TEMA_PADRAO).palette;
    renderDashboard();
    const valor = await screen.findByText(/125\.000,50/);

    expect(canaisDaCor(getComputedStyle(valor).color)).toEqual(canaisDaCor(palette.success.main));
  });
});
