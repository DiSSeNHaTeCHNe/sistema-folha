import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
import Rubricas from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { rubricaService } from '../../services/rubricaService';

const sampleRubrica = {
  id: 1,
  codigo: '001',
  descricao: 'Salário Base',
  tipo: 'PROVENTO' as const,
  tipoRubricaDescricao: 'PROVENTO',
  ativo: true,
  operadorBruto: 1,
  operadorLiquido: 1,
  operadorCusto: 1,
  porcentagem: 100,
};

vi.mock('../../services/rubricaService', () => ({
  rubricaService: {
    listar: vi.fn(),
    cadastrar: vi.fn(),
    atualizar: vi.fn(),
    remover: vi.fn(),
  },
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('Rubricas page', () => {
  beforeEach(() => {
    vi.mocked(rubricaService.listar).mockResolvedValue([sampleRubrica]);
    vi.mocked(rubricaService.remover).mockClear();
  });

  async function fillCreateForm(dialog: HTMLElement) {
    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Código' }), { target: { value: '002' } });
    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Descrição' }), { target: { value: 'Nova' } });
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(screen.getByRole('option', { name: 'Provento' }));
  }

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Rubricas />);

    expect(screen.getByRole('heading', { name: 'Rubricas' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova rubrica/i })).toBeInTheDocument();
    });
  });

  it('shows rubrica rows after loading', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByText('Salário Base')).toBeInTheDocument();
    });
    expect(screen.getByText('001')).toBeInTheDocument();
  });

  it('shows empty state when no rubricas', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([]);

    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByText('Nenhuma rubrica encontrada')).toBeInTheDocument();
    });
  });

  it('opens create dialog', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova rubrica/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nova rubrica/i }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Nova Rubrica' })).toBeInTheDocument();
  });

  it('opens edit dialog with mapped tipo', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByText('Salário Base')).toBeInTheDocument();
    });

    const editButton = screen.getByTestId('EditIcon').closest('button')!;
    fireEvent.click(editButton);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Editar Rubrica' })).toBeInTheDocument();
    });
  });

  it('submits filter form', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole('textbox', { name: 'Id/Código' }), { target: { value: '001' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));

    await waitFor(() => {
      expect(rubricaService.listar).toHaveBeenCalled();
    });
  });

  it('clears filters', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));

    await waitFor(() => {
      expect(rubricaService.listar).toHaveBeenCalled();
    });
  });

  it('deletes rubrica after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(rubricaService.remover).mockResolvedValue(undefined);

    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByText('Salário Base')).toBeInTheDocument();
    });

    const deleteButton = screen.getByTestId('DeleteIcon').closest('button')!;
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(rubricaService.remover).toHaveBeenCalledWith(1);
    });
    expect(toast.success).toHaveBeenCalledWith('Rubrica excluída com sucesso');
  });

  it('shows toast error when load fails', async () => {
    vi.mocked(rubricaService.listar).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar rubricas');
    });
  });

  it('closes dialog on cancel', async () => {
    renderWithProviders(<Rubricas />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova rubrica/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /nova rubrica/i }));
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('creates rubrica successfully', async () => {
    vi.mocked(rubricaService.cadastrar).mockResolvedValue(sampleRubrica);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova rubrica/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /nova rubrica/i }));
    const dialog = await screen.findByRole('dialog');
    await fillCreateForm(dialog);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => {
      expect(rubricaService.cadastrar).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Rubrica cadastrada com sucesso');
    });
  });

  it('updates rubrica successfully', async () => {
    vi.mocked(rubricaService.atualizar).mockResolvedValue(sampleRubrica);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Salário Base')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Atualizar' }));
    await waitFor(() => {
      expect(rubricaService.atualizar).toHaveBeenCalledWith(1, expect.any(Object));
      expect(toast.success).toHaveBeenCalledWith('Rubrica atualizada com sucesso');
    });
  });

  it('shows save error toast', async () => {
    vi.mocked(rubricaService.cadastrar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Rubricas />);
    fireEvent.click(await screen.findByRole('button', { name: /nova rubrica/i }));
    const dialog = await screen.findByRole('dialog');
    await fillCreateForm(dialog);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao salvar rubrica'));
  });

  it('shows filter error toast', async () => {
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    vi.mocked(rubricaService.listar).mockRejectedValue(new Error('fail'));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao filtrar rubricas'));
  });

  it('does not delete when confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Salário Base')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('DeleteIcon').closest('button')!);
    expect(rubricaService.remover).not.toHaveBeenCalled();
  });

  it('shows delete error toast', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(rubricaService.remover).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Salário Base')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('DeleteIcon').closest('button')!);
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao excluir rubrica'));
  });

  it('maps DESCONTO tipo when editing', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      { ...sampleRubrica, tipo: 'DESCONTO', tipoRubricaDescricao: 'DESCONTO', descricao: 'INSS' },
    ]);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('INSS')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    expect(await screen.findByRole('heading', { name: 'Editar Rubrica' })).toBeInTheDocument();
  });

  it('maps INFORMATIVO tipo when editing', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      { ...sampleRubrica, tipo: 'INFORMATIVO', tipoRubricaDescricao: 'INFORMATIVO', descricao: 'Info' },
    ]);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Info')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    expect(await screen.findByRole('heading', { name: 'Editar Rubrica' })).toBeInTheDocument();
  });

  it('filters by inactive status', async () => {
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    fireEvent.mouseDown(screen.getAllByRole('combobox')[0]);
    fireEvent.click(screen.getByRole('option', { name: 'Inativo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(rubricaService.listar).toHaveBeenCalledWith(expect.objectContaining({ status: 'INATIVO' })));
  });

  it('shows inactive rubrica without optional fields', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      {
        ...sampleRubrica,
        ativo: false,
        porcentagem: undefined,
        tipoRubricaDescricao: undefined,
        tipo: undefined as unknown as typeof sampleRubrica.tipo,
        descricao: 'Sem tipo',
      },
    ]);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Inativo')).toBeInTheDocument());
    expect(screen.getAllByText('-').length).toBeGreaterThan(0);
  });

  it('filters by todos status', async () => {
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    fireEvent.mouseDown(screen.getAllByRole('combobox')[0]);
    fireEvent.click(screen.getByRole('option', { name: 'Todos' }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(rubricaService.listar).toHaveBeenCalledWith(expect.objectContaining({ status: 'TODOS' })));
  });

  it('edits rubrica using tipo field when descricao tipo is missing', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      {
        ...sampleRubrica,
        tipo: 'DESCONTO',
        tipoRubricaDescricao: undefined,
        descricao: 'INSS',
      },
    ]);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('INSS')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    expect(await screen.findByRole('heading', { name: 'Editar Rubrica' })).toBeInTheDocument();
  });

  it('edits rubrica with undefined operadores', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      {
        ...sampleRubrica,
        operadorBruto: undefined,
        operadorLiquido: undefined,
        operadorCusto: undefined,
      },
    ]);
    vi.mocked(rubricaService.atualizar).mockResolvedValue(sampleRubrica);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Salário Base')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Atualizar' }));
    await waitFor(() => expect(rubricaService.atualizar).toHaveBeenCalled());
  });

  it('changes operador selects on create', async () => {
    vi.mocked(rubricaService.cadastrar).mockResolvedValue(sampleRubrica);
    renderWithProviders(<Rubricas />);
    fireEvent.click(await screen.findByRole('button', { name: /nova rubrica/i }));
    const dialog = await screen.findByRole('dialog');
    await fillCreateForm(dialog);
    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[1]);
    fireEvent.click(screen.getByRole('option', { name: '0 (ignora)' }));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(rubricaService.cadastrar).toHaveBeenCalled());
  });

  it('maps INFORMATIVO tipo when editing by descricao', async () => {
    vi.mocked(rubricaService.listar).mockResolvedValue([
      {
        ...sampleRubrica,
        tipo: 'INFORMATIVO',
        tipoRubricaDescricao: 'INFORMATIVO',
        descricao: 'Informativa',
      },
    ]);
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByText('Informativa')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('EditIcon').closest('button')!);
    expect(await screen.findByRole('heading', { name: 'Editar Rubrica' })).toBeInTheDocument();
  });

  it('opens create dialog with default operador selects', async () => {
    renderWithProviders(<Rubricas />);
    fireEvent.click(await screen.findByRole('button', { name: /nova rubrica/i }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Operador Bruto')).toBeInTheDocument();
    expect(within(dialog).getByText('Operador Líquido')).toBeInTheDocument();
    expect(within(dialog).getByText('Operador Custo')).toBeInTheDocument();
    expect(within(dialog).getAllByRole('combobox')).toHaveLength(4);
  });

  it('filters by descricao field', async () => {
    renderWithProviders(<Rubricas />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeEnabled());
    fireEvent.change(screen.getByRole('textbox', { name: 'Descrição' }), { target: { value: 'Salário' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(rubricaService.listar).toHaveBeenCalledWith(expect.objectContaining({ descricao: 'Salário' })));
  });
});
