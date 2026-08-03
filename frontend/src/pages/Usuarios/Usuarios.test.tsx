import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import Usuarios from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import usuarioService from '../../services/usuarioService';

const showNotification = vi.fn();

const sampleUsuario = {
  id: 1,
  login: 'admin',
  nome: 'Administrador',
  permissoes: ['ADMIN', 'API_KEY'],
  funcionarioId: 10,
  funcionarioNome: 'Maria Silva',
  funcionarioCpf: '12345678901',
};

vi.mock('../../services/usuarioService', () => ({
  default: {
    listar: vi.fn(),
    listarFuncionarios: vi.fn(),
    criar: vi.fn(),
    atualizar: vi.fn(),
    excluir: vi.fn(),
  },
}));

vi.mock('../../hooks/useNotification', () => ({
  useNotification: () => ({
    notification: { open: false, message: '', severity: 'info' },
    showNotification,
    hideNotification: vi.fn(),
  }),
}));

function setupMocks() {
  vi.mocked(usuarioService.listar).mockResolvedValue([sampleUsuario]);
  vi.mocked(usuarioService.listarFuncionarios).mockResolvedValue([
    { id: 10, nome: 'Maria Silva', cpf: '12345678901' },
  ]);
}

describe('Usuarios page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupMocks();
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Usuarios />);
    expect(screen.getByRole('heading', { name: 'Manutenção de Usuários' })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
  });

  it('shows empty state when no users', async () => {
    vi.mocked(usuarioService.listar).mockResolvedValue([]);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('Nenhum usuário encontrado')).toBeInTheDocument());
  });

  it('shows load error notification', async () => {
    vi.mocked(usuarioService.listar).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao carregar dados', 'error'));
  });

  it('opens new user dialog with API_KEY permission', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));
    expect(screen.getByRole('checkbox', { name: 'API_KEY' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Novo Usuário' })).toBeInTheDocument();
  });

  it('creates user with valid data', async () => {
    vi.mocked(usuarioService.criar).mockResolvedValue(sampleUsuario);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));

    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Login'), { target: { value: 'novo' } });
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Novo User' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'CONSULTA' }));
    fireEvent.change(within(dialog).getByLabelText('Senha'), { target: { value: 'senha123' } });
    fireEvent.change(within(dialog).getByLabelText(/Confirmar Senha/), { target: { value: 'senha123' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => {
      expect(usuarioService.criar).toHaveBeenCalled();
      expect(showNotification).toHaveBeenCalledWith('Usuário cadastrado com sucesso', 'success');
    });
  });

  it('rejects mismatched passwords', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));

    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Login'), { target: { value: 'novo' } });
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Novo' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'CONSULTA' }));
    fireEvent.change(within(dialog).getByLabelText('Senha'), { target: { value: 'senha123' } });
    fireEvent.change(within(dialog).getByLabelText(/Confirmar Senha/), { target: { value: 'outra' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('As senhas não coincidem', 'error'));
  });

  it('rejects short password', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));

    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Login'), { target: { value: 'novo' } });
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Novo' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'CONSULTA' }));
    fireEvent.change(within(dialog).getByLabelText('Senha'), { target: { value: '123' } });
    fireEvent.change(within(dialog).getByLabelText(/Confirmar Senha/), { target: { value: '123' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('A senha deve ter pelo menos 6 caracteres', 'error'));
  });

  it('edits user without changing password', async () => {
    vi.mocked(usuarioService.atualizar).mockResolvedValue(sampleUsuario);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());

    fireEvent.click(screen.getAllByTestId('EditIcon')[0].closest('button')!);
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Admin Editado' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Atualizar' }));

    await waitFor(() => {
      expect(usuarioService.atualizar).toHaveBeenCalledWith(1, expect.not.objectContaining({ senha: expect.anything() }));
      expect(showNotification).toHaveBeenCalledWith('Usuário atualizado com sucesso', 'success');
    });
  });

  it('deletes user when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(usuarioService.excluir).mockResolvedValue(undefined);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTestId('DeleteIcon')[0].closest('button')!);
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Usuário excluído com sucesso', 'success'));
  });

  it('does not delete when cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTestId('DeleteIcon')[0].closest('button')!);
    expect(usuarioService.excluir).not.toHaveBeenCalled();
  });

  it('filters users', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Admin' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(usuarioService.listar).toHaveBeenCalledWith(expect.objectContaining({ nome: 'Admin' })));
  });

  it('clears filters', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Limpar' }));
    await waitFor(() => expect(usuarioService.listar).toHaveBeenCalledTimes(2));
  });

  it('shows filter error', async () => {
    vi.mocked(usuarioService.listar).mockResolvedValueOnce([sampleUsuario]).mockRejectedValueOnce(new Error('fail'));
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Filtrar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao filtrar usuários', 'error'));
  });

  it('toggles password visibility', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));
    const dialog = await screen.findByRole('dialog');
    const senha = within(dialog).getByLabelText('Senha');
    expect(senha).toHaveAttribute('type', 'password');
    fireEvent.click(within(dialog).getAllByTestId('VisibilityIcon')[0].closest('button')!);
    expect(senha).toHaveAttribute('type', 'text');
  });

  it('shows save error from API', async () => {
    vi.mocked(usuarioService.criar).mockRejectedValue({ response: { data: { message: 'Login duplicado' } } });
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Login'), { target: { value: 'dup' } });
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Dup' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'CONSULTA' }));
    fireEvent.change(within(dialog).getByLabelText('Senha'), { target: { value: 'senha123' } });
    fireEvent.change(within(dialog).getByLabelText(/Confirmar Senha/), { target: { value: 'senha123' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Login duplicado', 'error'));
  });

  it('shows user without linked funcionario', async () => {
    vi.mocked(usuarioService.listar).mockResolvedValue([
      { ...sampleUsuario, funcionarioNome: undefined, funcionarioCpf: undefined },
    ]);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('Não vinculado')).toBeInTheDocument());
  });

  it('shows unknown permission chip color as default', async () => {
    vi.mocked(usuarioService.listar).mockResolvedValue([
      { ...sampleUsuario, permissoes: ['CUSTOM_PERM'] },
    ]);
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('CUSTOM_PERM')).toBeInTheDocument());
  });

  it('unchecks permission checkbox on edit', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTestId('EditIcon')[0].closest('button')!);
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'API_KEY' }));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Atualizar' }));
    await waitFor(() => expect(usuarioService.atualizar).toHaveBeenCalled());
  });

  it('toggles confirm password visibility', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));
    const dialog = await screen.findByRole('dialog');
    const confirm = within(dialog).getByLabelText(/Confirmar Senha/);
    expect(confirm).toHaveAttribute('type', 'password');
    fireEvent.click(within(dialog).getAllByTestId('VisibilityIcon')[1].closest('button')!);
    expect(confirm).toHaveAttribute('type', 'text');
  });

  it('shows fallback save error without API message', async () => {
    vi.mocked(usuarioService.criar).mockRejectedValue('err');
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByRole('button', { name: /novo usuário/i })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /novo usuário/i }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Login'), { target: { value: 'x' } });
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'X' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'CONSULTA' }));
    fireEvent.change(within(dialog).getByLabelText('Senha'), { target: { value: 'senha123' } });
    fireEvent.change(within(dialog).getByLabelText(/Confirmar Senha/), { target: { value: 'senha123' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao salvar usuário', 'error'));
  });

  it('shows delete error notification', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(usuarioService.excluir).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTestId('DeleteIcon')[0].closest('button')!);
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao excluir usuário', 'error'));
  });

  it('filters by funcionario select', async () => {
    renderWithProviders(<Usuarios />);
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    const combobox = screen.getAllByRole('combobox')[0];
    fireEvent.mouseDown(combobox);
    fireEvent.click(screen.getByRole('option', { name: /Maria Silva/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }));
    await waitFor(() =>
      expect(usuarioService.listar).toHaveBeenCalledWith(expect.objectContaining({ funcionarioId: 10 })),
    );
  });
});
