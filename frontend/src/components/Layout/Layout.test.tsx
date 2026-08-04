import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { Layout } from './index';
import { defaultMockAuth, renderWithProviders } from '../../test/renderWithProviders';

const mockLogout = vi.fn();

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    ...defaultMockAuth,
    user: { id: 1, login: 'admin', nome: 'Admin User', permissoes: ['ADMIN'] },
    isAuthenticated: true,
    logout: mockLogout,
  }),
}));

vi.mock('../AlterarSenhaDialog', () => ({
  AlterarSenhaDialog: ({ open }: { open: boolean }) =>
    open ? <div role="dialog" aria-label="alterar-senha">Alterar Senha</div> : null,
}));

vi.mock('../AparenciaDialog', () => ({
  AparenciaDialog: ({ open }: { open: boolean }) =>
    open ? <div role="dialog" aria-label="aparencia">Aparência</div> : null,
}));

function renderLayout() {
  return renderWithProviders(
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<div>Page content</div>} />
      </Route>
    </Routes>,
    { route: '/' },
  );
}

describe('Layout', () => {
  it('renders app title and main navigation items', () => {
    renderLayout();

    expect(screen.getAllByText('Sistema de Folha').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Dashboard').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Funcionários').length).toBeGreaterThan(0);
  });

  it('shows admin cadastros menu for admin users', () => {
    renderLayout();

    expect(screen.getAllByText('Cadastros').length).toBeGreaterThan(0);
  });

  it('expands cadastros submenu when clicked', () => {
    renderLayout();

    fireEvent.click(screen.getAllByText('Cadastros')[0]);

    expect(screen.getAllByText('Usuários').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Rubricas Fixas').length).toBeGreaterThan(0);
  });

  it('opens user menu and alterar senha dialog', () => {
    renderLayout();

    fireEvent.click(screen.getByRole('button', { name: 'account of current user' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Alterar senha' }));

    expect(screen.getByRole('dialog', { name: 'alterar-senha' })).toBeInTheDocument();
  });

  it('shows Aparencia menu item above Alterar senha', () => {
    renderLayout();

    fireEvent.click(screen.getByRole('button', { name: 'account of current user' }));

    const menuItems = screen.getAllByRole('menuitem').map((item) => item.textContent);
    const aparenciaIndex = menuItems.indexOf('Aparência');
    const alterarSenhaIndex = menuItems.indexOf('Alterar senha');

    expect(aparenciaIndex).toBeGreaterThan(-1);
    expect(alterarSenhaIndex).toBeGreaterThan(aparenciaIndex);
  });

  it('opens aparencia dialog from avatar menu', () => {
    renderLayout();

    fireEvent.click(screen.getByRole('button', { name: 'account of current user' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Aparência' }));

    expect(screen.getByRole('dialog', { name: 'aparencia' })).toBeInTheDocument();
  });

  it('renders outlet content', () => {
    renderLayout();

    expect(screen.getByText('Page content')).toBeInTheDocument();
  });
});
