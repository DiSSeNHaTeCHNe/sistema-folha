import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import Importacao from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/importacaoService', () => ({
  importacaoService: {
    importarFolhaAdp: vi.fn(),
    importarBeneficios: vi.fn(),
  },
}));

vi.mock('../../services/beneficioMensalService', () => ({
  beneficioMensalService: {
    listarCompetencias: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/folhaPagamentoService', () => ({
  folhaPagamentoService: {
    listarCompetencias: vi.fn().mockResolvedValue([]),
  },
}));

describe('Importacao page', () => {
  it('renders the page title without real HTTP', () => {
    renderWithProviders(<Importacao />);

    expect(screen.getByRole('heading', { name: 'Importação de Dados' })).toBeInTheDocument();
  });
});
