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
});
