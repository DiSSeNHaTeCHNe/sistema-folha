import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
import Funcionarios from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { funcionarioService } from '../../services/funcionarioService';
import { cargoService } from '../../services/cargoService';
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';

const sampleFuncionario = {
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
};

const sampleCargo = { id: 1, descricao: 'Analista', ativo: true };
const sampleCentro = { id: 1, descricao: 'TI', ativo: true, linhaNegocioId: 1 };
const sampleLinha = { id: 1, descricao: 'Tecnologia' };

vi.mock('../../services/funcionarioService', () => ({
  funcionarioService: {
    listar: vi.fn(),
    criar: vi.fn(),
    atualizar: vi.fn(),
    remover: vi.fn(),
    filtrar: vi.fn(),
  },
}));

vi.mock('../../services/cargoService', () => ({
  cargoService: { listarTodos: vi.fn() },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: { listarTodos: vi.fn() },
}));

vi.mock('../../services/linhaNegocioService', () => ({
  linhaNegocioService: { listarTodos: vi.fn() },
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

function setupMocks() {
  vi.mocked(funcionarioService.listar).mockResolvedValue([sampleFuncionario]);
  vi.mocked(cargoService.listarTodos).mockResolvedValue([sampleCargo]);
  vi.mocked(centroCustoService.listarTodos).mockResolvedValue([sampleCentro]);
  vi.mocked(linhaNegocioService.listarTodos).mockResolvedValue([sampleLinha]);
  vi.mocked(funcionarioService.filtrar).mockResolvedValue([sampleFuncionario]);
}

describe('Funcionarios page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupMocks();
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Funcionarios />);
    expect(screen.getByRole('heading', { name: 'Funcionários' })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeInTheDocument();
    });
  });

  it('shows funcionario card after loading', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(screen.getByText('João Silva')).toBeInTheDocument();
    });
  });

  it('shows toast when load fails', async () => {
    vi.mocked(funcionarioService.listar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar dados');
    });
  });

  it('shows toast when cargos load fails', async () => {
    vi.mocked(cargoService.listarTodos).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar cargos');
    });
  });

  it('shows toast when centros de custo load fails', async () => {
    vi.mocked(centroCustoService.listarTodos).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar centros de custo');
    });
  });

  it('submits filter form', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.change(screen.getByRole('textbox', { name: 'Nome' }), { target: { value: 'João' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(funcionarioService.filtrar).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('1 funcionário(s) encontrado(s)');
    });
  });

  it('shows toast when filter fails', async () => {
    vi.mocked(funcionarioService.filtrar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao filtrar funcionários');
    });
  });

  it('clears filters and reloads', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Limpar Filtros' }));

    await waitFor(() => {
      expect(funcionarioService.listar).toHaveBeenCalledTimes(2);
    });
  });

  it('opens create dialog', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());

    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Novo Funcionário' })).toBeInTheDocument();
    });
  });

  it('opens edit dialog from card button', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Editar Funcionário' })).toBeInTheDocument();
    });
  });

  it('shows hover details with formatted date', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const card = screen.getByText('João Silva').closest('.MuiCard-root')!;
    fireEvent.mouseEnter(card);

    await waitFor(() => {
      expect(screen.getByText(/15\/01\/2024/)).toBeInTheDocument();
      expect(screen.getByText(/MAT001/)).toBeInTheDocument();
    });

    fireEvent.mouseLeave(card);
  });

  it('shows inactive chip for inactive funcionario', async () => {
    vi.mocked(funcionarioService.listar).mockResolvedValue([
      { ...sampleFuncionario, ativo: false },
    ]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(screen.getByLabelText('Inativo')).toBeInTheDocument();
    });
  });

  it('shows empty admission date on hover when missing', async () => {
    vi.mocked(funcionarioService.listar).mockResolvedValue([
      { ...sampleFuncionario, dataAdmissao: '' },
    ]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.mouseEnter(screen.getByText('João Silva').closest('.MuiCard-root')!);

    await waitFor(() => {
      expect(screen.queryByText(/15\/01\/2024/)).not.toBeInTheDocument();
    });
  });

  it('shows N/A for missing optional fields on hover', async () => {
    vi.mocked(funcionarioService.listar).mockResolvedValue([
      {
        ...sampleFuncionario,
        cargoDescricao: '',
        centroCustoDescricao: '',
        linhaNegocioDescricao: '',
        idExterno: undefined,
        dataAdmissao: '15/01/2024',
      },
    ]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.mouseEnter(screen.getByText('João Silva').closest('.MuiCard-root')!);

    await waitFor(() => {
      const naLabels = screen.getAllByText('N/A');
      expect(naLabels.length).toBeGreaterThan(0);
    });
  });

  it('inactivates funcionario after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioService.remover).mockResolvedValue(undefined);

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));

    await waitFor(() => {
      expect(funcionarioService.remover).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Funcionário inativado com sucesso');
      expect(toast.info).toHaveBeenCalled();
    });
  });

  it('does not inactivate when confirm is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));

    expect(funcionarioService.remover).not.toHaveBeenCalled();
  });

  it('shows error when inactivate fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioService.remover).mockRejectedValue({
      response: { data: { message: 'Não permitido' } },
    });

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Não permitido');
    });
  });

  it('creates funcionario successfully', async () => {
    vi.mocked(funcionarioService.criar).mockResolvedValue(sampleFuncionario);

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));

    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Maria' } });
    fireEvent.change(within(dialog).getByLabelText('CPF'), { target: { value: '98765432100' } });
    fireEvent.change(within(dialog).getByLabelText('Data de Admissão'), {
      target: { value: '2024-06-01' },
    });

    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[0]);
    await waitFor(() => screen.getByRole('option', { name: 'Analista' }));
    fireEvent.click(screen.getByRole('option', { name: 'Analista' }));

    fireEvent.mouseDown(comboboxes[1]);
    await waitFor(() => screen.getByRole('option', { name: 'TI' }));
    fireEvent.click(screen.getByRole('option', { name: 'TI' }));

    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(funcionarioService.criar).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Funcionário cadastrado com sucesso');
    });
  });

  it('updates funcionario successfully', async () => {
    vi.mocked(funcionarioService.atualizar).mockResolvedValue(sampleFuncionario);

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Editar Funcionário' })).toBeInTheDocument());

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(funcionarioService.atualizar).toHaveBeenCalledWith(1, expect.any(Object));
      expect(toast.success).toHaveBeenCalledWith('Funcionário atualizado com sucesso');
    });
  });

  it('shows API error message on save failure', async () => {
    vi.mocked(funcionarioService.atualizar).mockRejectedValue({
      response: { data: { message: 'CPF duplicado' } },
    });

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('CPF duplicado');
    });
  });

  it('shows fallback error on save failure without response', async () => {
    vi.mocked(funcionarioService.atualizar).mockRejectedValue(new Error('network'));

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao salvar funcionário');
    });
  });

  it('closes dialog on cancel', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('submits filter with linha de negocio selected', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const linhaCombobox = screen.getAllByRole('combobox')[0];
    fireEvent.mouseDown(linhaCombobox);
    fireEvent.click(screen.getByRole('option', { name: 'Tecnologia' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => expect(funcionarioService.filtrar).toHaveBeenCalled());
  });

  it('submits filter with cargo selected', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const cargoCombobox = screen.getAllByRole('combobox')[1];
    fireEvent.mouseDown(cargoCombobox);
    fireEvent.click(screen.getByRole('option', { name: 'Analista' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => expect(funcionarioService.filtrar).toHaveBeenCalled());
  });

  it('submits filter with centro de custo selected', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const centroCombobox = screen.getAllByRole('combobox')[2];
    fireEvent.mouseDown(centroCombobox);
    fireEvent.click(screen.getByRole('option', { name: 'TI' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => expect(funcionarioService.filtrar).toHaveBeenCalled());
  });

  it('submits filter with status TODOS', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const statusCombobox = screen.getAllByRole('combobox')[3];
    fireEvent.mouseDown(statusCombobox);
    fireEvent.click(screen.getByRole('option', { name: 'Todos' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => expect(funcionarioService.filtrar).toHaveBeenCalled());
  });

  it('initializes edit form with empty strings when optional fields are missing', async () => {
    vi.mocked(funcionarioService.listar).mockResolvedValue([
      {
        ...sampleFuncionario,
        cargoId: undefined as unknown as number,
        centroCustoId: undefined as unknown as number,
        linhaNegocioId: undefined as unknown as number,
        idExterno: undefined,
        dataAdmissao: '',
      },
    ]);

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));

    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(within(dialog).getByLabelText('Nome')).toHaveValue('João Silva'));
    expect(within(dialog).getByLabelText('Data de Admissão')).toHaveValue('');
  });

  it('uses fallback when error response data is not an object', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioService.remover).mockRejectedValue({ response: { data: 'erro' } });

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao inativar funcionário');
    });
  });

  it('trims blank idExterno on create', async () => {
    vi.mocked(funcionarioService.criar).mockResolvedValue(sampleFuncionario);

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));
    const dialog = await screen.findByRole('dialog');

    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Maria' } });
    fireEvent.change(within(dialog).getByLabelText('CPF'), { target: { value: '98765432100' } });
    fireEvent.change(within(dialog).getByLabelText('Data de Admissão'), { target: { value: '2024-06-01' } });
    fireEvent.change(within(dialog).getByLabelText('ID Externo (matrícula ADP)'), { target: { value: '   ' } });

    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[0]);
    await waitFor(() => screen.getByRole('option', { name: 'Analista' }));
    fireEvent.click(screen.getByRole('option', { name: 'Analista' }));
    fireEvent.mouseDown(comboboxes[1]);
    await waitFor(() => screen.getByRole('option', { name: 'TI' }));
    fireEvent.click(screen.getByRole('option', { name: 'TI' }));

    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(funcionarioService.criar).toHaveBeenCalledWith(
        expect.objectContaining({ idExterno: undefined }),
      );
    });
  });

  it('shows empty list when no funcionarios', async () => {
    vi.mocked(funcionarioService.listar).mockResolvedValue([]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => {
      expect(screen.queryByText('João Silva')).not.toBeInTheDocument();
    });
  });

  it('shows zero count toast when filter returns empty', async () => {
    vi.mocked(funcionarioService.filtrar).mockResolvedValue([]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('0 funcionário(s) encontrado(s)');
    });
  });

  it('uses fallback when API error has response without message', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioService.remover).mockRejectedValue({ response: { data: {} } });

    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao inativar funcionário');
    });
  });

  it('changes status filter to INATIVO', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());

    const statusCombobox = screen.getAllByRole('combobox').find(
      (el) => el.textContent?.includes('Ativo'),
    )!;
    fireEvent.mouseDown(statusCombobox);
    fireEvent.click(screen.getByRole('option', { name: 'Inativo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => expect(funcionarioService.filtrar).toHaveBeenCalled());
  });

  it('does not clear centro when editing existing funcionario and cargo changes', async () => {
    vi.mocked(cargoService.listarTodos).mockResolvedValue([
      sampleCargo,
      { id: 2, descricao: 'Gerente', ativo: true },
    ]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    const comboboxes = within(screen.getByRole('dialog')).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[0]);
    await waitFor(() => screen.getByRole('option', { name: 'Gerente' }));
    fireEvent.click(screen.getByRole('option', { name: 'Gerente' }));

    expect(comboboxes[1]).toHaveTextContent('TI');
  });

  it('sets linha de negocio when centro with linha is selected on create', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    const comboboxes = within(screen.getByRole('dialog')).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[1]);
    await waitFor(() => screen.getByRole('option', { name: 'TI' }));
    fireEvent.click(screen.getByRole('option', { name: 'TI' }));

    expect(comboboxes[2]).toHaveTextContent('Tecnologia');
  });

  it('handles centro without linha de negocio on create', async () => {
    vi.mocked(centroCustoService.listarTodos).mockResolvedValue([
      { id: 2, descricao: 'RH', ativo: true, linhaNegocioId: 0 },
    ]);
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    const comboboxes = within(screen.getByRole('dialog')).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[1]);
    await waitFor(() => screen.getByRole('option', { name: 'RH' }));
    fireEvent.click(screen.getByRole('option', { name: 'RH' }));
  });

  it('clears centro and linha when cargo changes on new funcionario', async () => {
    renderWithProviders(<Funcionarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo funcionário/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

    const dialog = screen.getByRole('dialog');
    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[1]);
    await waitFor(() => screen.getByRole('option', { name: 'TI' }));
    fireEvent.click(screen.getByRole('option', { name: 'TI' }));

    fireEvent.mouseDown(comboboxes[0]);
    await waitFor(() => screen.getByRole('option', { name: 'Analista' }));
    fireEvent.click(screen.getByRole('option', { name: 'Analista' }));

    expect(comboboxes[1]).not.toHaveTextContent('TI');
  });
});
