import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import BeneficiosMensais from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { beneficioMensalService } from '../../services/beneficioMensalService';
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';

const sampleResumo = {
  competenciaInicio: '2026-01-01',
  competenciaFim: '2026-01-31',
  totalFuncionarios: 2,
  totalBeneficios: 1500,
  qtdLancamentos: 3,
};

const sampleLancamento = {
  id: 1,
  funcionarioId: 10,
  funcionarioNome: 'Maria Silva',
  competenciaInicio: '2026-01-01',
  competenciaFim: '2026-01-31',
  valor: 500,
  tipoBeneficioCodigo: 'VR',
  tipoBeneficioDescricao: 'Vale Refeição',
  observacao: 'Lunch',
  cargoDescricao: 'Analista',
  centroCustoDescricao: 'TI',
  linhaNegocioDescricao: 'Corporate',
};

vi.mock('../../services/beneficioMensalService', () => ({
  beneficioMensalService: {
    listarCompetencias: vi.fn(),
    listar: vi.fn(),
  },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: {
    listarTodos: vi.fn().mockResolvedValue([
      { id: 1, descricao: 'TI' },
      { id: 2, descricao: 'RH' },
    ]),
  },
}));

vi.mock('../../services/linhaNegocioService', () => ({
  linhaNegocioService: {
    listarTodos: vi.fn().mockResolvedValue([
      { id: 1, descricao: 'Corporate' },
    ]),
  },
}));

describe('BeneficiosMensais page', () => {
  beforeEach(() => {
    vi.mocked(beneficioMensalService.listarCompetencias).mockResolvedValue([sampleResumo]);
    vi.mocked(beneficioMensalService.listar).mockResolvedValue([sampleLancamento]);
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<BeneficiosMensais />);

    expect(screen.getByRole('heading', { name: 'Benefícios Mensais' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });
  });

  it('shows resumo rows with formatted competencia and totals', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByText('01/01/2026 a 31/01/2026')).toBeInTheDocument();
    });
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
  });

  it('shows empty message when no resumos are returned', async () => {
    vi.mocked(beneficioMensalService.listarCompetencias).mockResolvedValue([]);

    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByText('Nenhum benefício mensal encontrado.')).toBeInTheDocument();
    });
  });

  it('refetches resumos when filter is submitted', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('spinbutton', { name: 'Mês' }), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(beneficioMensalService.listarCompetencias).toHaveBeenCalledWith(
        expect.any(Number),
        1,
      );
    });
  });

  it('clears resumo filters and reloads data', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));

    await waitFor(() => {
      expect(beneficioMensalService.listarCompetencias).toHaveBeenCalled();
    });
  });

  it('loads funcionarios after selecting a resumo', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });
    expect(beneficioMensalService.listar).toHaveBeenCalledWith({
      competenciaInicio: '2026-01-01',
      competenciaFim: '2026-01-31',
    });
  });

  it('returns to resumos list from funcionarios view', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: '← Voltar' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Resumos de Benefícios Mensais' })).toBeInTheDocument();
    });
  });

  it('filters funcionarios by search text', async () => {
    vi.mocked(beneficioMensalService.listar).mockResolvedValue([
      sampleLancamento,
      { ...sampleLancamento, id: 2, funcionarioId: 11, funcionarioNome: 'Pedro Souza' },
    ]);

    renderWithProviders(<BeneficiosMensais />);

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

  it('opens detalhes dialog when Ver Benefícios is clicked', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Benefícios' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Benefícios' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByText('Vale Refeição')).toBeInTheDocument();
    expect(screen.getByText('VR')).toBeInTheDocument();
  });

  it('closes detalhes dialog', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Benefícios' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Benefícios' }));
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('shows error when resumo fetch fails', async () => {
    vi.mocked(beneficioMensalService.listarCompetencias).mockRejectedValue(new Error('network'));

    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByText('Erro ao buscar resumos de benefícios')).toBeInTheDocument();
    });
  });

  it('loads filter options from centro and linha services', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(centroCustoService.listarTodos).toHaveBeenCalled();
      expect(linhaNegocioService.listarTodos).toHaveBeenCalled();
    });
  });

  it('filters funcionarios by linha de negocio', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });

    fireEvent.mouseDown(screen.getByLabelText('Linha de Negócio'));
    fireEvent.click(await screen.findByRole('option', { name: 'Corporate' }));

    expect(screen.getByText('Maria Silva')).toBeInTheDocument();
  });

  it('filters funcionarios by centro de custo', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });

    fireEvent.mouseDown(screen.getByLabelText('Centro de Custo'));
    fireEvent.click(await screen.findByRole('option', { name: 'TI' }));

    expect(screen.getByText('Maria Silva')).toBeInTheDocument();
  });

  it('clears funcionario filters', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar funcionário' }), {
      target: { value: 'maria' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));

    expect(screen.getByRole('textbox', { name: 'Buscar funcionário' })).toHaveValue('');
  });

  it('shows error when funcionarios fetch fails', async () => {
    vi.mocked(beneficioMensalService.listar).mockRejectedValue(new Error('network'));

    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));

    await waitFor(() => {
      expect(screen.getByText('Erro ao buscar funcionários')).toBeInTheDocument();
    });
  });

  it('shows empty funcionarios message when filter excludes all', async () => {
    renderWithProviders(<BeneficiosMensais />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => {
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar funcionário' }), {
      target: { value: 'inexistente' },
    });

    expect(screen.getByText('Nenhum funcionário encontrado para este período.')).toBeInTheDocument();
  });

  it('shows error when filter options fail to load', async () => {
    vi.mocked(centroCustoService.listarTodos).mockRejectedValue(new Error('fail'));
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Benefícios Mensais' })).toBeInTheDocument());
  });

  it('shows beneficios in detalhes dialog', async () => {
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Ver Benefícios' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
  });

  it('aggregates multiple lancamentos for same funcionario', async () => {
    vi.mocked(beneficioMensalService.listar).mockResolvedValue([
      sampleLancamento,
      { ...sampleLancamento, id: 2, valor: 300 },
    ]);
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument());
    expect(screen.getByText(/R\$\s*800,00/)).toBeInTheDocument();
  });

  it('renders preformatted competencia dates', async () => {
    vi.mocked(beneficioMensalService.listarCompetencias).mockResolvedValue([
      { ...sampleResumo, competenciaInicio: '01/01/2026', competenciaFim: '31/01/2026' },
    ]);
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByText('01/01/2026 a 31/01/2026')).toBeInTheDocument());
  });

  it('filters resumos by month', async () => {
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Mês' }), { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(beneficioMensalService.listarCompetencias).toHaveBeenCalledWith(expect.any(Number), 3));
  });

  it('shows resumo fetch error', async () => {
    vi.mocked(beneficioMensalService.listarCompetencias).mockRejectedValue(new Error('fail'));
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByText('Erro ao buscar resumos de benefícios')).toBeInTheDocument());
  });

  it('shows funcionario card with missing optional metadata', async () => {
    vi.mocked(beneficioMensalService.listar).mockResolvedValue([
      {
        ...sampleLancamento,
        funcionarioNome: undefined,
        cargoDescricao: undefined,
        centroCustoDescricao: undefined,
        linhaNegocioDescricao: undefined,
      },
    ]);
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    await waitFor(() => expect(screen.getByText(/Cargo:/)).toBeInTheDocument());
    expect(screen.getByText(/Centro de Custo:/)).toBeInTheDocument();
  });

  it('shows detalhes dialog with missing benefit fields', async () => {
    vi.mocked(beneficioMensalService.listar).mockResolvedValue([
      {
        ...sampleLancamento,
        tipoBeneficioCodigo: undefined,
        tipoBeneficioDescricao: undefined,
        observacao: undefined,
      },
    ]);
    renderWithProviders(<BeneficiosMensais />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Ver Funcionários' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Ver Funcionários' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Ver Benefícios' }));
    await waitFor(() => expect(screen.getAllByText('—').length).toBeGreaterThan(0));
  });
});
