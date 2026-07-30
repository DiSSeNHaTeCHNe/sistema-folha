import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { FolhaPagamento } from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';

const sampleResumo = {
  id: 42,
  competenciaInicio: '2024-10-01',
  competenciaFim: '2024-10-31',
  totalEmpregados: 5,
  totalEncargos: 100,
  totalPagamentos: 1000,
  totalDescontos: 200,
  totalBruto: 1000,
  totalLiquido: 800,
  totalCustoEmpresa: 1200,
  decimoTerceiro: false,
};

const sampleResumoDecimo = {
  ...sampleResumo,
  id: 43,
  decimoTerceiro: true,
};

const sampleFuncionarioTotal = {
  funcionarioId: 10,
  funcionarioNome: 'Maria Silva',
  competenciaInicio: '2024-10-01',
  competenciaFim: '2024-10-31',
  totalRubricas: 3,
  salBruto: 5000,
  salLiquido: 4000,
  salCustoFolha: 4500,
  salCustoBeneficios: 200,
  custoEmpresa: 5200,
  cargoDescricao: 'Analista',
  centroCustoDescricao: 'TI',
  linhaNegocioDescricao: 'Corporate',
};

vi.mock('../../services/resumoFolhaPagamentoService', () => ({
  resumoFolhaPagamentoService: {
    listarPorAno: vi.fn(),
  },
}));

vi.mock('../../services/folhaPagamentoService', () => ({
  folhaPagamentoService: {
    consultarTotaisPorFuncionario: vi.fn(),
    buscarFichaPorFuncionario: vi.fn(),
    listarLinhasPorTotalizador: vi.fn(),
    listarLinhasDetalhe: vi.fn(),
  },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: {
    listarTodos: vi.fn().mockResolvedValue([
      { id: 1, descricao: 'Centro A' },
      { id: 2, descricao: 'Centro B' },
    ]),
  },
}));

vi.mock('../../services/linhaNegocioService', () => ({
  linhaNegocioService: {
    listarTodos: vi.fn().mockResolvedValue([
      { id: 1, descricao: 'Linha A' },
    ]),
  },
}));

describe('FolhaPagamento page', () => {
  beforeEach(() => {
    vi.mocked(resumoFolhaPagamentoService.listarPorAno).mockResolvedValue([sampleResumo]);
    vi.mocked(folhaPagamentoService.consultarTotaisPorFuncionario).mockResolvedValue([sampleFuncionarioTotal]);
    vi.mocked(folhaPagamentoService.buscarFichaPorFuncionario).mockResolvedValue(99);
    vi.mocked(folhaPagamentoService.listarLinhasPorTotalizador).mockResolvedValue([]);
    vi.mocked(folhaPagamentoService.listarLinhasDetalhe).mockResolvedValue([]);
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<FolhaPagamento />);

    expect(screen.getByRole('heading', { name: 'Folha de Pagamento' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Resumos da Folha de Pagamento' })).toBeInTheDocument();
    });
  });

  it('shows summary filter fields after loading', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('spinbutton', { name: 'Mês' })).toBeInTheDocument();
    });
    expect(screen.getByRole('combobox', { name: 'Ano' })).toBeInTheDocument();
  });

  it('shows the filter action button on the main tab', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });
  });

  it('renders resumo rows with formatted competencia and totals', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByText('01/10/2024 a 31/10/2024')).toBeInTheDocument();
    });
    expect(screen.getByText('Normal')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
  });

  it('loads funcionarios after selecting a resumo', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });
    expect(folhaPagamentoService.consultarTotaisPorFuncionario).toHaveBeenCalledWith(
      '2024-10-01',
      '2024-10-31',
      false,
    );
  });

  it('shows decimo terceiro badge for thirteenth salary resumos', async () => {
    vi.mocked(resumoFolhaPagamentoService.listarPorAno).mockResolvedValue([sampleResumoDecimo]);

    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByText('13º')).toBeInTheDocument();
    });
  });

  it('refetches resumos when filter is submitted', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('spinbutton', { name: 'Mês' }), { target: { value: '10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(resumoFolhaPagamentoService.listarPorAno).toHaveBeenCalled();
    });
  });

  it('opens rubricas dialog when Ver Rubricas is clicked', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Rubricas' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Ver Rubricas' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(folhaPagamentoService.buscarFichaPorFuncionario).toHaveBeenCalled();
  });

  it('shows empty resumos message when none are returned', async () => {
    vi.mocked(resumoFolhaPagamentoService.listarPorAno).mockResolvedValue([]);

    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByText('Nenhum resumo de folha de pagamento encontrado.')).toBeInTheDocument();
    });
  });

  it('returns to resumos list from funcionarios view', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: '← Voltar' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Resumos da Folha de Pagamento' })).toBeInTheDocument();
    });
  });

  it('clears resumo filters and reloads data', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));

    await waitFor(() => {
      expect(resumoFolhaPagamentoService.listarPorAno).toHaveBeenCalled();
    });
  });

  it('filters funcionarios by search text', async () => {
    vi.mocked(folhaPagamentoService.consultarTotaisPorFuncionario).mockResolvedValue([
      sampleFuncionarioTotal,
      { ...sampleFuncionarioTotal, funcionarioId: 11, funcionarioNome: 'Pedro Souza' },
    ]);

    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
      expect(screen.getByText('Pedro Souza')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar funcionário' }), {
      target: { value: 'maria' },
    });

    expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    expect(screen.queryByText('Pedro Souza')).not.toBeInTheDocument();
  });

  it('shows ficha missing message when rubricas lookup returns null', async () => {
    vi.mocked(folhaPagamentoService.buscarFichaPorFuncionario).mockResolvedValue(null);

    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Rubricas' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Rubricas' }));

    await waitFor(() => {
      expect(
        screen.getByText('Ficha não processada para este funcionário. Execute o processamento da competência.'),
      ).toBeInTheDocument();
    });
  });

  it('switches totalizer tabs in the rubricas dialog', async () => {
    vi.mocked(folhaPagamentoService.listarLinhasPorTotalizador).mockResolvedValue([
      {
        rubricaCodigo: '0010',
        rubricaDescricao: 'Salario Base',
        origemLinha: 'FOLHA_ADP',
        contribuicao: 1000,
        porcentagem: 100,
      },
    ]);

    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Rubricas' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Rubricas' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('tab', { name: 'Líquido' }));

    await waitFor(() => {
      expect(folhaPagamentoService.listarLinhasPorTotalizador).toHaveBeenCalledWith(99, 'NET');
    });
  });

  it('uses stable list keys without Math.random (S2245 regression)', async () => {
    const source = await import('./index?raw');
    expect(source.default).not.toMatch(/Math\.random\s*\(/);
    expect(source.default).toMatch(/key=\{[^}]+\.id\}/);
  });
});
