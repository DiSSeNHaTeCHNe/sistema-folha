import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
import RubricasFixas from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { funcionarioRubricaFixaService } from '../../services/funcionarioRubricaFixaService';
import { funcionarioService } from '../../services/funcionarioService';
import { rubricaService } from '../../services/rubricaService';

const sampleRegistro = {
  id: 1,
  funcionarioId: null,
  funcionarioNome: null,
  rubricaId: 1,
  rubricaCodigo: '001',
  rubricaDescricao: 'Salário',
  valor: '1000.00',
  porcentagem: null,
  vigenciaInicio: '2026-01-01',
  vigenciaFim: null,
  comentario: null,
};

const sampleFuncionario = { id: 10, nome: 'Maria Silva', cpf: '123', ativo: true };
const sampleRubrica = {
  id: 1,
  codigo: '001',
  descricao: 'Salário',
  tipo: 'PROVENTO' as const,
  tipoRubricaDescricao: 'PROVENTO',
  ativo: true,
  operadorBruto: 1,
  operadorLiquido: 1,
  operadorCusto: 1,
};

vi.mock('../../services/funcionarioRubricaFixaService', () => ({
  funcionarioRubricaFixaService: {
    listar: vi.fn(),
    criar: vi.fn(),
    atualizar: vi.fn(),
    remover: vi.fn(),
  },
}));

vi.mock('../../services/funcionarioService', () => ({
  funcionarioService: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/rubricaService', () => ({
  rubricaService: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('RubricasFixas page', () => {
  beforeEach(() => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([sampleRegistro]);
    vi.mocked(funcionarioService.listar).mockResolvedValue([sampleFuncionario]);
    vi.mocked(rubricaService.listar).mockResolvedValue([sampleRubrica]);
    vi.mocked(funcionarioRubricaFixaService.remover).mockClear();
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<RubricasFixas />);

    expect(screen.getByRole('heading', { name: 'Rubricas Fixas' })).toBeInTheDocument();
  });

  it('shows 100% when porcentagem is null', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('cell', { name: '100%' })).toBeInTheDocument();
    });
  });

  it('shows Todos for global rubrica fixa', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('cell', { name: 'Todos' })).toBeInTheDocument();
    });
  });

  it('shows empty state when no registros', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([]);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByText('Nenhuma rubrica fixa encontrada')).toBeInTheDocument();
    });
  });

  it('opens create dialog from Nova Rubrica Fixa button', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Nova Rubrica Fixa' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Nova Rubrica Fixa' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Nova Rubrica Fixa' })).toBeInTheDocument();
  });

  it('opens edit dialog with existing values', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Editar rubrica fixa' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Editar rubrica fixa' }));

    await waitFor(() => {
      expect(screen.getByText('Editar Rubrica Fixa')).toBeInTheDocument();
    });
  });

  it('submits filter form and reloads registros', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(funcionarioRubricaFixaService.listar).toHaveBeenCalled();
    });
  });

  it('clears filters and reloads registros', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));

    await waitFor(() => {
      expect(funcionarioRubricaFixaService.listar).toHaveBeenCalled();
    });
  });

  it('deletes a rubrica fixa after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioRubricaFixaService.remover).mockResolvedValue(undefined);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Excluir rubrica fixa' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Excluir rubrica fixa' }));

    await waitFor(() => {
      expect(funcionarioRubricaFixaService.remover).toHaveBeenCalledWith(1);
    });
    expect(toast.success).toHaveBeenCalledWith('Rubrica fixa excluída com sucesso');
  });

  it('does not delete when confirmation is cancelled', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Excluir rubrica fixa' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Excluir rubrica fixa' }));

    expect(funcionarioRubricaFixaService.remover).not.toHaveBeenCalled();
    confirmSpy.mockRestore();
  });

  it('shows open vigencia when vigenciaFim is null', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByText(/2026-01-01 \(aberta\)/)).toBeInTheDocument();
    });
  });

  it('shows toast error when initial load fails', async () => {
    vi.mocked(funcionarioService.listar).mockRejectedValue(new Error('fail'));

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar dados iniciais');
    });
  });

  it('shows funcionario name when funcionarioId is set', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([
      { ...sampleRegistro, id: 2, funcionarioId: 10, funcionarioNome: 'Maria Silva' },
    ]);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('cell', { name: 'Maria Silva' })).toBeInTheDocument();
    });
  });

  it('shows closed vigencia range when vigenciaFim is set', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([
      { ...sampleRegistro, vigenciaFim: '2026-12-31' },
    ]);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByText('2026-01-01 a 2026-12-31')).toBeInTheDocument();
    });
  });

  it('shows custom percentual when porcentagem is provided', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([
      { ...sampleRegistro, porcentagem: 50 },
    ]);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('cell', { name: '50%' })).toBeInTheDocument();
    });
  });

  it('updates rubrica fixa from dialog submit', async () => {
    vi.mocked(funcionarioRubricaFixaService.atualizar).mockResolvedValue(sampleRegistro);

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Editar rubrica fixa' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Editar rubrica fixa' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Atualizar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }));

    await waitFor(() => {
      expect(funcionarioRubricaFixaService.atualizar).toHaveBeenCalledWith(1, expect.any(Object));
    });
    expect(toast.success).toHaveBeenCalledWith('Rubrica fixa atualizada com sucesso');
  });

  it('shows toast error when delete fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(funcionarioRubricaFixaService.remover).mockRejectedValue(new Error('fail'));

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Excluir rubrica fixa' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Excluir rubrica fixa' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao excluir rubrica fixa');
    });
  });

  it('shows generic error when save fails without API message', async () => {
    vi.mocked(funcionarioRubricaFixaService.atualizar).mockRejectedValue(new Error('network'));

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Editar rubrica fixa' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Editar rubrica fixa' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Atualizar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao salvar rubrica fixa');
    });
  });

  it('shows toast error when list load fails', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockRejectedValue(new Error('fail'));

    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar rubricas fixas');
    });
  });

  it('closes dialog on cancel', async () => {
    renderWithProviders(<RubricasFixas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Nova Rubrica Fixa' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Nova Rubrica Fixa' }));
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('shows Todos for global rubrica fixa', async () => {
    renderWithProviders(<RubricasFixas />);
    await waitFor(() => expect(screen.getByRole('cell', { name: 'Todos' })).toBeInTheDocument());
  });

  it('creates rubrica fixa', async () => {
    vi.mocked(funcionarioRubricaFixaService.criar).mockResolvedValue(sampleRegistro);
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Nova Rubrica Fixa' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(screen.getByRole('option', { name: '001 - Salário' }));
    fireEvent.change(within(dialog).getByLabelText('Valor'), { target: { value: '1000' } });
    fireEvent.change(within(dialog).getByLabelText(/Vigência início/i), { target: { value: '2026-01-01' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(funcionarioRubricaFixaService.criar).toHaveBeenCalled());
  });

  it('shows API error message on save failure', async () => {
    vi.mocked(funcionarioRubricaFixaService.atualizar).mockRejectedValue({
      response: { data: { message: 'Vigência inválida' } },
    });
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Editar rubrica fixa' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Atualizar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Vigência inválida'));
  });

  it('does not delete when confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Excluir rubrica fixa' }));
    expect(funcionarioRubricaFixaService.remover).not.toHaveBeenCalled();
  });

  it('opens edit dialog with optional empty fields', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([
      {
        ...sampleRegistro,
        valor: null,
        comentario: null,
        vigenciaFim: null,
        porcentagem: null,
      },
    ]);
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Editar rubrica fixa' }));
    expect(await screen.findByRole('heading', { name: 'Editar Rubrica Fixa' })).toBeInTheDocument();
  });

  it('shows funcionario id when nome is missing', async () => {
    vi.mocked(funcionarioRubricaFixaService.listar).mockResolvedValue([
      { ...sampleRegistro, funcionarioId: 99, funcionarioNome: null },
    ]);
    renderWithProviders(<RubricasFixas />);
    await waitFor(() => expect(screen.getByRole('cell', { name: '99' })).toBeInTheDocument());
  });

  it('selects specific funcionario on create', async () => {
    vi.mocked(funcionarioRubricaFixaService.criar).mockResolvedValue({
      ...sampleRegistro,
      funcionarioId: 10,
      funcionarioNome: 'Maria Silva',
    });
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Nova Rubrica Fixa' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(screen.getByRole('option', { name: '001 - Salário' }));
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[1]);
    fireEvent.click(screen.getByRole('option', { name: 'Maria Silva' }));
    fireEvent.change(within(dialog).getByLabelText('Valor'), { target: { value: '1000' } });
    fireEvent.change(within(dialog).getByLabelText(/Vigência início/i), { target: { value: '2026-01-01' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() =>
      expect(funcionarioRubricaFixaService.criar).toHaveBeenCalledWith(
        expect.objectContaining({ funcionarioId: 10 }),
      ),
    );
  });

  it('shows fallback error when API response is not an object', async () => {
    vi.mocked(funcionarioRubricaFixaService.atualizar).mockRejectedValue({ response: { data: 'erro' } });
    renderWithProviders(<RubricasFixas />);
    fireEvent.click(await screen.findByRole('button', { name: 'Editar rubrica fixa' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Atualizar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao salvar rubrica fixa'));
  });

  it('submits filter form', async () => {
    renderWithProviders(<RubricasFixas />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(funcionarioRubricaFixaService.listar).toHaveBeenCalled());
  });
});
