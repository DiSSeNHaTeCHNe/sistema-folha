import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { FolhaPagamento } from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/resumoFolhaPagamentoService', () => ({
  resumoFolhaPagamentoService: {
    listarPorAno: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('../../services/folhaPagamentoService', () => ({
  folhaPagamentoService: {
    listarLinhasDetalhe: vi.fn().mockResolvedValue([]),
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

describe('FolhaPagamento page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<FolhaPagamento />);

    expect(screen.getByRole('heading', { name: 'Folha de Pagamento' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Resumos da Folha de Pagamento' })).toBeInTheDocument();
    });
  });

  it('shows summary filter fields after loading', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('spinbutton', { name: 'Mês' })).toBeInTheDocument();
    });
    expect(screen.getByRole('combobox', { name: 'Ano' })).toBeInTheDocument();
  });

  it('shows the filter action button on the main tab', async () => {
    renderWithProviders(<FolhaPagamento />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument();
    });
  });

  it('uses stable list keys without Math.random (S2245 regression)', async () => {
    const source = await import('./index?raw');
    expect(source.default).not.toMatch(/Math\.random\s*\(/);
    expect(source.default).toMatch(/key=\{[^}]+\.id\}/);
  });
});
