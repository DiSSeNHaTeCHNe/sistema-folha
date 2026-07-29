import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Funcionarios from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/funcionarioService', () => ({
  funcionarioService: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/cargoService', () => ({
  cargoService: {
    listarTodos: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: {
    listarTodos: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/linhaNegocioService', () => ({
  linhaNegocioService: {
    listarTodos: vi.fn().mockResolvedValue([]),
  },
}));

describe('Funcionarios page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Funcionarios />);

    expect(screen.getByRole('heading', { name: 'Funcionários' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /novo funcionário/i })).toBeInTheDocument();
    });
  });
});
