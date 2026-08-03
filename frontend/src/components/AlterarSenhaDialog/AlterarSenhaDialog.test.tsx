import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AlterarSenhaDialog } from './index';
import usuarioService from '../../services/usuarioService';

const mockOnClose = vi.fn();

vi.mock('../../services/usuarioService', () => ({
  default: {
    alterarSenha: vi.fn(),
  },
}));

describe('AlterarSenhaDialog', () => {
  beforeEach(() => {
    mockOnClose.mockClear();
    vi.mocked(usuarioService.alterarSenha).mockReset();
  });

  it('renders the dialog title and password fields when open', () => {
    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    expect(screen.getByRole('heading', { name: 'Alterar senha' })).toBeInTheDocument();
    expect(screen.getByLabelText('Senha atual')).toBeInTheDocument();
    expect(screen.getByLabelText('Nova senha')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirmar nova senha')).toBeInTheDocument();
  });

  it('shows incorrect current password alert on 400 response', async () => {
    vi.mocked(usuarioService.alterarSenha).mockRejectedValue({ response: { status: 400 } });

    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'wrong' } });
    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'secret1' } });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Senha atual incorreta');
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  it('closes the dialog after a successful password change', async () => {
    vi.mocked(usuarioService.alterarSenha).mockResolvedValue(undefined);

    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'oldpass' } });
    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'secret1' } });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' }));

    await waitFor(() => {
      expect(usuarioService.alterarSenha).toHaveBeenCalledWith(1, 'oldpass', 'secret1');
    });
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('calls onClose when cancel is clicked', () => {
    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(mockOnClose).toHaveBeenCalled();
  });

  it('shows validation when passwords mismatch', async () => {
    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'oldpass' } });
    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'secret1' } });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: 'other' } });
    fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(await screen.findByText('As senhas não coincidem')).toBeInTheDocument();
  });

  it('shows generic error for non-400 failures', async () => {
    vi.mocked(usuarioService.alterarSenha).mockRejectedValue(new Error('fail'));

    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'oldpass' } });
    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'secret1' } });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(await screen.findByText('Erro ao alterar senha')).toBeInTheDocument();
  });

  it('shows password length validation', async () => {
    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);

    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'oldpass' } });
    fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: '12345' } });
    fireEvent.change(screen.getByLabelText('Confirmar nova senha'), { target: { value: '12345' } });
    fireEvent.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(await screen.findByText('A senha deve ter pelo menos 6 caracteres')).toBeInTheDocument();
  });

  it('resets form when dialog reopens', () => {
    const { rerender } = render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);
    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'typed' } });
    rerender(<AlterarSenhaDialog open={false} onClose={mockOnClose} userId={1} />);
    rerender(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);
    expect(screen.getByLabelText('Senha atual')).toHaveValue('');
  });

  it('toggles visibility for nova and confirmar senha fields', () => {
    render(<AlterarSenhaDialog open onClose={mockOnClose} userId={1} />);
    const [novaToggle, confirmarToggle] = screen
      .getAllByRole('button')
      .filter((btn) => btn.querySelector('svg'));
    fireEvent.click(novaToggle);
    fireEvent.click(confirmarToggle);
    expect(screen.getByLabelText('Nova senha')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Confirmar nova senha')).toHaveAttribute('type', 'text');
    fireEvent.click(novaToggle);
    fireEvent.click(confirmarToggle);
    expect(screen.getByLabelText('Nova senha')).toHaveAttribute('type', 'password');
  });
});
