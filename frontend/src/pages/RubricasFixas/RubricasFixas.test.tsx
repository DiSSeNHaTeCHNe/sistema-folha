import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
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
});
