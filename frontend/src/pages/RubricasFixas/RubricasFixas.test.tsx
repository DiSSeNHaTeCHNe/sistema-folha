import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import RubricasFixas from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/funcionarioRubricaFixaService', () => ({
  funcionarioRubricaFixaService: {
    listar: vi.fn().mockResolvedValue([
      {
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
      },
    ]),
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

describe('RubricasFixas page', () => {
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
});
