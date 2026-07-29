import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import Rubricas from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('../../services/rubricaService', () => ({
  rubricaService: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

describe('Rubricas page', () => {
  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Rubricas />);

    expect(screen.getByRole('heading', { name: 'Rubricas' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova rubrica/i })).toBeInTheDocument();
    });
  });
});
