import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
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

vi.mock('react-toastify', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

import apiKeyService from '../../services/apiKeyService';
import usuarioService from '../../services/usuarioService';

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

const adminWithApiKey: Usuario = {
  id: 3,
  login: 'adminkey',
  nome: 'Admin Key',
  permissoes: ['ADMIN', 'API_KEY'],
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

const revokedKey: ApiKeyListItem = {
  ...activeKey,
  id: 11,
  nome: 'Revogada',
  revogado: true,
  ultimoUsoEm: '2026-06-01T12:00:00',
};

describe('ApiKeys page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiKeyService.listar).mockResolvedValue([]);
  });

  it('disables create button for ADMIN without API_KEY permission', async () => {
    mockUseAuth.mockReturnValue({ user: adminOnlyUser, loading: false });
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova api key/i })).toBeDisabled());
    expect(screen.getByText('Conceda a permissão API_KEY ao seu usuário para criar chaves.')).toBeInTheDocument();
  });

  it('enables create button for user with API_KEY permission', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova api key/i })).toBeEnabled());
  });

  it('lists keys and revokes active key', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(apiKeyService.revogar).mockResolvedValue(undefined);

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByText('Integração')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /revogar api key integração/i }));

    await waitFor(() => {
      expect(apiKeyService.revogar).toHaveBeenCalledWith(10);
      expect(toast.success).toHaveBeenCalledWith('API Key revogada');
    });
  });

  it('does not revoke when cancelled', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByText('Integração')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /revogar api key integração/i }));
    expect(apiKeyService.revogar).not.toHaveBeenCalled();
  });

  it('creates api key and shows secret dialog', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockResolvedValue({
      id: 99,
      nome: 'Nova',
      prefixo: 'sf_test',
      chave: 'secret-key-value',
      dataExpiracao: '2027-01-01',
    });

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova api key/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /nova api key/i }));

    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(within(dialog).getByRole('textbox', { name: 'Nome' })).toBeInTheDocument());
    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Criar' }));

    await waitFor(() => {
      expect(apiKeyService.criar).toHaveBeenCalledWith({ nome: 'Nova', diasValidade: 365 });
      expect(toast.success).toHaveBeenCalledWith('API Key criada com sucesso');
      expect(screen.getByDisplayValue('secret-key-value')).toBeInTheDocument();
    });
  });

  it('validates create form fields', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova api key/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /nova api key/i }));
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Criar' }));
    expect(toast.error).toHaveBeenCalledWith('Informe um nome para a API Key');

    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'X' } });
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('spinbutton', { name: 'Validade (dias)' }), { target: { value: '0' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));
    expect(toast.error).toHaveBeenCalledWith('Validade deve estar entre 1 e 365 dias');
  });

  it('copies secret to clipboard', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockResolvedValue({
      id: 99,
      nome: 'Nova',
      prefixo: 'sf_test',
      chave: 'secret-key-value',
      dataExpiracao: '2027-01-01',
    });
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    });

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('button', { name: /nova api key/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /nova api key/i }));
    const createDialog = await screen.findByRole('dialog');
    fireEvent.change(within(createDialog).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(createDialog).getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(screen.getByDisplayValue('secret-key-value')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Copiar' }));
    await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Secret copiado para a área de transferência'));
  });

  it('shows copy error when clipboard fails', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockResolvedValue({
      id: 99,
      nome: 'Nova',
      prefixo: 'sf_test',
      chave: 'secret-key-value',
      dataExpiracao: '2027-01-01',
    });
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockRejectedValue(new Error('fail')) },
    });

    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));
    await waitFor(() => expect(screen.getByDisplayValue('secret-key-value')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Copiar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Não foi possível copiar o secret'));
  });

  it('shows load keys error', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao carregar API Keys'));
  });

  it('shows empty keys message', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByText('Nenhuma API Key encontrada')).toBeInTheDocument());
  });

  it('shows revoked key without revoke button', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([revokedKey]);
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('row', { name: /Revogada/ })).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /revogar api key revogada/i })).not.toBeInTheDocument();
  });

  it('admin can select usuario and load keys', async () => {
    mockUseAuth.mockReturnValue({ user: adminWithApiKey, loading: false });
    vi.mocked(usuarioService.listar).mockResolvedValue([adminWithApiKey, apiKeyUser]);
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByLabelText('Usuário')).toBeInTheDocument());
    fireEvent.mouseDown(screen.getByLabelText('Usuário'));
    fireEvent.click(screen.getByRole('option', { name: /API User/ }));
    await waitFor(() => expect(apiKeyService.listar).toHaveBeenCalled());
  });

  it('shows create error from API', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockRejectedValue({ response: { data: { message: 'Limite excedido' } } });

    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Limite excedido'));
  });

  it('shows revoke error', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(apiKeyService.revogar).mockRejectedValue(new Error('fail'));

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByText('Integração')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /revogar api key integração/i }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao revogar API Key'));
  });

  it('admin load usuarios error', async () => {
    mockUseAuth.mockReturnValue({ user: adminWithApiKey, loading: false });
    vi.mocked(usuarioService.listar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao carregar usuários'));
  });

  it('shows create fallback error without API message', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockRejectedValue('fail');
    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao criar API Key'));
  });

  it('does nothing when copying without secret', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    const writeText = vi.fn();
    Object.assign(navigator, { clipboard: { writeText } });
    vi.mocked(apiKeyService.criar).mockResolvedValue({
      id: 99,
      nome: 'Nova',
      prefixo: 'sf_test',
      chave: '',
      dataExpiracao: '2027-01-01',
    });
    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Copiar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Copiar' }));
    expect(writeText).not.toHaveBeenCalled();
  });

  it('closes create dialog on cancel', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('closes secret dialog on fechar', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.criar).mockResolvedValue({
      id: 99,
      nome: 'Nova',
      prefixo: 'sf_test',
      chave: 'secret-key-value',
      dataExpiracao: '2027-01-01',
    });
    renderWithProviders(<ApiKeys />);
    fireEvent.click(await screen.findByRole('button', { name: /nova api key/i }));
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Nome' }), { target: { value: 'Nova' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));
    await waitFor(() => expect(screen.getByDisplayValue('secret-key-value')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));
    await waitFor(() => expect(screen.queryByDisplayValue('secret-key-value')).not.toBeInTheDocument());
  });

  it('shows revoke error from API message', async () => {
    mockUseAuth.mockReturnValue({ user: apiKeyUser, loading: false });
    vi.mocked(apiKeyService.listar).mockResolvedValue([activeKey]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(apiKeyService.revogar).mockRejectedValue({ response: { data: { message: 'Já revogada' } } });

    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByText('Integração')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /revogar api key integração/i }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Já revogada'));
  });

  it('skips loading keys when user id is missing', async () => {
    mockUseAuth.mockReturnValue({ user: { ...apiKeyUser, id: undefined as unknown as number }, loading: false });
    renderWithProviders(<ApiKeys />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'API Keys' })).toBeInTheDocument());
    expect(apiKeyService.listar).not.toHaveBeenCalled();
  });
});
