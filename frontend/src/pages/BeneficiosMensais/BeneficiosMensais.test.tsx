import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import BeneficiosMensais from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/beneficioMensalService', () => ({
  beneficioMensalService: {
    listarCompetencias: vi.fn().mockResolvedValue([]),
    listar: vi.fn().mockResolvedValue([]),
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

describe('BeneficiosMensais page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<BeneficiosMensais />);

    expect(screen.getByRole('heading', { name: 'Benefícios Mensais' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });
  });
});
