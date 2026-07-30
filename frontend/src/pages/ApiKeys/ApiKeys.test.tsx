import { describe, expect, it, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import ApiKeys from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import type { Usuario } from '../../types';
import type { ApiKeyListItem } from '../../services/apiKeyService';

const mockUseAuth = vi.fn();

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../services/apiKeyService', () => ({
  default: {
    listar: vi.fn(),
    criar: vi.fn(),
    revogar: vi.fn(),
  },
}));

vi.mock('../../services/usuarioService', () => ({
  default: {
    listar: vi.fn().mockResolvedValue([]),
  },
}));

import apiKeyService from '../../services/apiKeyService';

const adminOnlyUser: Usuario = {
  id: 2,
  login: 'admin',
  nome: 'Admin',
  permissoes: ['ADMIN'],
};

const apiKeyUser: Usuario = {
  id: 1,
  login: 'apiuser',
  nome: 'API User',
  permissoes: ['API_KEY'],
};

const activeKey: ApiKeyListItem = {
  id: 10,
  nome: 'Integração',
  prefixo: 'sf_live_abc',
  dataExpiracao: '2027-01-01T00:00:00',
  revogado: false,
  escopo: 'READ_ONLY',
  ultimoUsoEm: null,
  dataCriacao: '2026-01-01T00:00:00',
};

describe('ApiKeys page', () => {
  beforeEach(() => {
    vi.mocked(apiKeyService.listar).mockResolvedValue([]);
  });

  it('disables create button and shows warning for ADMIN without API_KEY permission', async () => {
    mockUseAuth.mockReturnValue({ user: adminOnlyUser, loading: false });

    renderWithProviders(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova api key/i })).toBeDisabled();
    });

    expect(
      screen.getByText('Conceda a permissão API_KEY ao seu usuário para criar chaves.'),
    ).toBeInTheDocument();
  });

  it('enables create button for user with API_KEY permission', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });

    renderWithProviders(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nova api key/i })).toBeEnabled();
    });

    expect(
      screen.queryByText('Conceda a permissão API_KEY ao seu usuário para criar chaves.'),
    ).not.toBeInTheDocument();
  });

  it('keeps revoke enabled for ADMIN without API_KEY when keys exist', async () => {
    mockUseAuth.mockReturnValue({ user: adminOnlyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);

    renderWithProviders(<ApiKeys />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /revogar api key integração/i })).toBeEnabled();
    });
  });
});
