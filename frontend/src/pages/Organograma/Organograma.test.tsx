import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Organograma from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/organogramaService', () => ({
  organogramaService: {
    listarTodos: vi.fn().mockResolvedValue([]),
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

describe('Organograma page', () => {
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
});
