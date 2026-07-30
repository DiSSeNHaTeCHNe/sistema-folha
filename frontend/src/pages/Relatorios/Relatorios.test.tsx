import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { Relatorios } from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/relatorioService', () => ({
  relatorioService: {
    listarRelatoriosFolha: vi.fn().mockResolvedValue([]),
    listarRelatoriosBeneficio: vi.fn().mockResolvedValue([]),
  },
}));

describe('Relatorios page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Relatorios />);

    expect(screen.getByRole('heading', { name: 'Relatórios' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Folha de Pagamento' })).toBeInTheDocument();
    });
  });
});
