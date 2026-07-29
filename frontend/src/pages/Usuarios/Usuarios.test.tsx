import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Usuarios from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/usuarioService', () => ({
  default: {
    listar: vi.fn().mockResolvedValue([]),
    listarFuncionarios: vi.fn().mockResolvedValue([]),
  },
}));

describe('Usuarios page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Usuarios />);

    expect(screen.getByRole('heading', { name: 'Manutenção de Usuários' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /novo usuário/i })).toBeInTheDocument();
    });
  });
});
