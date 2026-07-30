import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { toast } from 'react-toastify';
import Organograma from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { organogramaService } from '../../services/organogramaService';

const sampleNo = {
  id: 1,
  nome: 'Diretoria',
  descricao: 'Nó raiz',
  paiId: null,
  funcionarios: [{ id: 10, nome: 'Maria Silva', cpf: '123', ativo: true }],
  centrosCusto: [{ id: 1, descricao: 'TI', codigo: 'CC1' }],
};

vi.mock('../../services/organogramaService', () => ({
  organogramaService: {
    listarTodos: vi.fn(),
    cadastrar: vi.fn(),
    atualizar: vi.fn(),
    remover: vi.fn(),
  },
}));

vi.mock('../../services/funcionarioService', () => ({
  funcionarioService: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: {
    listarTodos: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../components/OrganogramaGrafico', () => ({
  default: () => <div role="region" aria-label="organograma-grafico-mock">Gráfico Mock</div>,
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('Organograma page', () => {
  beforeEach(() => {
    vi.mocked(organogramaService.listarTodos).mockResolvedValue([sampleNo]);
  });

  it('renders the page heading without real HTTP', async () => {
    renderWithProviders(<Organograma />);

    expect(screen.getByRole('heading', { name: /Organograma/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText('Carregando organograma...')).not.toBeInTheDocument();
    });
  });

  it('shows list view toggle', async () => {
    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Lista' })).toBeInTheDocument();
    });
  });

  it('shows graph view toggle', async () => {
    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Gráfico' })).toBeInTheDocument();
    });
  });

  it('displays organograma nodes in list view', async () => {
    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByText('Diretoria')).toBeInTheDocument();
    });
  });

  it('switches to graph view', async () => {
    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Gráfico' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Gráfico' }));

    await waitFor(() => {
      expect(screen.getByRole('region', { name: 'organograma-grafico-mock' })).toBeInTheDocument();
    });
  });

  it('opens create dialog from Novo Nó Raiz', async () => {
    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Novo Nó Raiz' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Novo Nó Raiz' }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByText('Criar Novo Nó')).toBeInTheDocument();
  });

  it('shows empty state when no nodes exist', async () => {
    vi.mocked(organogramaService.listarTodos).mockResolvedValue([]);

    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(screen.getByText(/Clique em "Novo Nó Raiz" para começar/)).toBeInTheDocument();
    });
  });

  it('shows toast error when loading fails', async () => {
    vi.mocked(organogramaService.listarTodos).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Organograma />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
  });
});
