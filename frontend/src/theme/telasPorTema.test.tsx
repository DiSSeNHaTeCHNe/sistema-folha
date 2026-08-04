import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { Login } from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Funcionarios from '../pages/Funcionarios';
import { FolhaPagamento } from '../pages/FolhaPagamento';
import Organograma from '../pages/Organograma';
import { defaultMockAuth, renderWithProviders } from '../test/renderWithProviders';
import type { TemaId } from './themes';
import { criarTema } from './themes';
import { getDashboardStats } from '../services/dashboardService';
import { funcionarioService } from '../services/funcionarioService';
import { cargoService } from '../services/cargoService';
import { centroCustoService } from '../services/centroCustoService';
import { linhaNegocioService } from '../services/linhaNegocioService';
import { resumoFolhaPagamentoService } from '../services/resumoFolhaPagamentoService';
import { folhaPagamentoService } from '../services/folhaPagamentoService';
import { organogramaService } from '../services/organogramaService';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    ...defaultMockAuth,
    login: vi.fn(),
  }),
}));

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

vi.mock('../services/dashboardService', () => ({
  getDashboardStats: vi.fn(),
}));

vi.mock('../hooks/useNotification', () => ({
  useNotification: () => ({
    notification: { open: false, message: '', severity: 'info' },
    showNotification: vi.fn(),
    hideNotification: vi.fn(),
  }),
}));

vi.mock('../services/funcionarioService', () => ({
  funcionarioService: {
    listar: vi.fn(),
    criar: vi.fn(),
    atualizar: vi.fn(),
    remover: vi.fn(),
    filtrar: vi.fn(),
  },
}));

vi.mock('../services/cargoService', () => ({
  cargoService: { listarTodos: vi.fn() },
}));

vi.mock('../services/centroCustoService', () => ({
  centroCustoService: { listarTodos: vi.fn() },
}));

vi.mock('../services/linhaNegocioService', () => ({
  linhaNegocioService: { listarTodos: vi.fn() },
}));

vi.mock('../services/resumoFolhaPagamentoService', () => ({
  resumoFolhaPagamentoService: { listarPorAno: vi.fn() },
}));

vi.mock('../services/folhaPagamentoService', () => ({
  folhaPagamentoService: {
    consultarTotaisPorFuncionario: vi.fn(),
    buscarFichaPorFuncionario: vi.fn(),
    listarLinhasPorTotalizador: vi.fn(),
    listarLinhasDetalhe: vi.fn(),
  },
}));

vi.mock('../services/organogramaService', () => ({
  organogramaService: {
    listarTodos: vi.fn(),
    criarNo: vi.fn(),
    atualizarNo: vi.fn(),
    removerNo: vi.fn(),
    adicionarFuncionario: vi.fn(),
    removerFuncionario: vi.fn(),
    adicionarCentroCusto: vi.fn(),
    removerCentroCusto: vi.fn(),
  },
}));

vi.mock('../components/OrganogramaGrafico', () => ({
  default: () => <div role="region" aria-label="organograma-grafico-mock" />,
}));

vi.mock('react-toastify', () => ({
  toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

vi.mock('@dnd-kit/core', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@dnd-kit/core')>();
  return {
    ...actual,
    DndContext: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    DragOverlay: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useDraggable: () => ({
      attributes: {},
      listeners: {},
      setNodeRef: vi.fn(),
      transform: null,
      isDragging: false,
    }),
    useDroppable: () => ({ setNodeRef: vi.fn(), isOver: false }),
  };
});

function setupServiceMocks() {
  vi.mocked(getDashboardStats).mockResolvedValue({
    totalFuncionarios: 1,
    custoMensalFolha: 1000,
    totalBeneficiosAtivos: 0,
    porLinhaNegocio: [],
    porCentroCusto: [],
    porCargo: [],
    totalProventos: 0,
    totalDescontos: 0,
    topProventos: [],
    topDescontos: [],
    evolucaoMensal: [],
  });

  vi.mocked(funcionarioService.listar).mockResolvedValue([
    {
      id: 1,
      nome: 'João Silva',
      cpf: '12345678901',
      dataAdmissao: '2024-01-15',
      cargoId: 1,
      cargoDescricao: 'Analista',
      centroCustoId: 1,
      centroCustoDescricao: 'TI',
      linhaNegocioId: 1,
      linhaNegocioDescricao: 'Tecnologia',
      idExterno: 'MAT001',
      ativo: true,
    },
  ]);
  vi.mocked(funcionarioService.filtrar).mockResolvedValue([]);
  vi.mocked(cargoService.listarTodos).mockResolvedValue([{ id: 1, descricao: 'Analista', ativo: true }]);
  vi.mocked(centroCustoService.listarTodos).mockResolvedValue([
    { id: 1, descricao: 'TI', ativo: true, linhaNegocioId: 1 },
  ]);
  vi.mocked(linhaNegocioService.listarTodos).mockResolvedValue([{ id: 1, descricao: 'Tecnologia' }]);

  vi.mocked(resumoFolhaPagamentoService.listarPorAno).mockResolvedValue([
    {
      id: 42,
      competenciaInicio: '2024-10-01',
      competenciaFim: '2024-10-31',
      totalEmpregados: 1,
      totalEncargos: 0,
      totalPagamentos: 1000,
      totalDescontos: 0,
      totalBruto: 1000,
      totalLiquido: 1000,
      totalCustoEmpresa: 1000,
      decimoTerceiro: false,
    },
  ]);
  vi.mocked(folhaPagamentoService.consultarTotaisPorFuncionario).mockResolvedValue([
    {
      funcionarioId: 10,
      funcionarioNome: 'Maria Silva',
      competenciaInicio: '2024-10-01',
      competenciaFim: '2024-10-31',
      totalRubricas: 1,
      salBruto: 5000,
      salLiquido: 4000,
      salCustoFolha: 4500,
      salCustoBeneficios: 0,
      custoEmpresa: 5200,
      cargoDescricao: 'Analista',
      centroCustoDescricao: 'TI',
      linhaNegocioDescricao: 'Corporate',
    },
  ]);
  vi.mocked(folhaPagamentoService.buscarFichaPorFuncionario).mockResolvedValue(99);
  vi.mocked(folhaPagamentoService.listarLinhasPorTotalizador).mockResolvedValue([]);
  vi.mocked(folhaPagamentoService.listarLinhasDetalhe).mockResolvedValue([]);

  vi.mocked(organogramaService.listarTodos).mockResolvedValue([
    {
      id: 1,
      nome: 'Diretoria',
      descricao: 'Raiz',
      nivel: 0,
      parentId: undefined,
      posicao: 0,
      ativo: true,
      funcionarioIds: [],
      centroCustoIds: [],
    },
  ]);
}

function telasSmoke(temaId: TemaId) {
  return [
    {
      nome: 'Login',
      render: () => renderWithProviders(<Login />, { temaId }),
      assert: () => {
        expect(screen.getByRole('heading', { name: 'Sistema de Folha' })).toBeInTheDocument();
      },
    },
    {
      nome: 'Dashboard',
      render: () =>
        renderWithProviders(<Dashboard />, {
          temaId,
          route: '/dashboard',
          routerProps: { initialEntries: ['/dashboard'] },
        }),
      assert: async () => {
        await waitFor(() => {
          expect(screen.getByRole('heading', { name: 'Dashboard Gerencial' })).toBeInTheDocument();
        });
      },
    },
    {
      nome: 'Funcionários',
      render: () => renderWithProviders(<Funcionarios />, { temaId }),
      assert: async () => {
        expect(screen.getByRole('heading', { name: 'Funcionários' })).toBeInTheDocument();
        await waitFor(() => {
          expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeInTheDocument();
        });
      },
    },
    {
      nome: 'Folha de Pagamento',
      render: () => renderWithProviders(<FolhaPagamento />, { temaId }),
      assert: async () => {
        expect(screen.getByRole('heading', { name: 'Folha de Pagamento' })).toBeInTheDocument();
        await waitFor(() => {
          expect(screen.getByRole('heading', { name: 'Resumos da Folha de Pagamento' })).toBeInTheDocument();
        });
      },
    },
    {
      nome: 'Organograma',
      render: () => renderWithProviders(<Organograma />, { temaId }),
      assert: async () => {
        expect(screen.getByRole('heading', { name: /Organograma/i })).toBeInTheDocument();
        await waitFor(() => {
          expect(screen.getByText('Diretoria')).toBeInTheDocument();
        });
      },
    },
  ] as const;
}

describe('varredura de telas por tema', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupServiceMocks();
  });

  describe.each(['corporate', 'soft', 'indigo', 'techne'] as const)('tema %s', (temaId) => {
    it.each(telasSmoke(temaId))('renderiza $nome sem erro', async ({ render, assert }) => {
      render();
      await assert();
    });
  });

  describe('tema techne — Folha de Pagamento', () => {
    it('renderiza com tipografia Poppins sem quebra de layout denso', async () => {
      expect(criarTema('techne').typography.fontFamily).toMatch(/^Poppins/);

      renderWithProviders(<FolhaPagamento />, { temaId: 'techne' });

      expect(screen.getByRole('heading', { name: 'Folha de Pagamento' })).toBeInTheDocument();
      await waitFor(() => {
        expect(
          screen.getByRole('heading', { name: 'Resumos da Folha de Pagamento' }),
        ).toBeInTheDocument();
      });
      expect(screen.getByRole('columnheader', { name: /competência/i })).toBeInTheDocument();
    });
  });
});
