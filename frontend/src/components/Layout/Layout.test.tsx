import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { Layout } from './index';
import { defaultMockAuth, renderWithProviders } from '../../test/renderWithProviders';
import type { AcessoUsuario } from '../../types';

const mockLogout = vi.fn();
let mockAcessoUsuario: AcessoUsuario | null = null;

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    ...defaultMockAuth,
    user: { id: 1, login: 'admin', nome: 'Admin User', permissoes: ['ADMIN'] },
    isAuthenticated: true,
    logout: mockLogout,
    acessoUsuario: mockAcessoUsuario,
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

function renderLayout(route = '/') {
  return renderWithProviders(
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<div>Page content</div>} />
        <Route path="workspace" element={<div>Workspace hub</div>} />
        <Route path="workspace/datasets" element={<div>Datasets list</div>} />
        <Route path="workspace/datasets/:id" element={<div>Dataset editor</div>} />
        <Route path="workspace/templates" element={<div>Template catalog</div>} />
        <Route path="workspace/:workspaceId" element={<div>Workspace detail</div>} />
      </Route>
    </Routes>,
    { route },
  );
}

describe('Layout', () => {
  beforeEach(() => {
    mockAcessoUsuario = null;
  });

  it('shows Dashboard and Meu Dashboard simultaneously for scoped user (DASHC-05)', () => {
    mockAcessoUsuario = {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: false,
      centrosCustoIds: [1, 2],
      quantidadeCentrosAcessiveis: 2,
    };
    renderLayout();

    expect(screen.getAllByText('Dashboard').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Meu Dashboard').length).toBeGreaterThan(0);
  });

  it('hides Meu Dashboard menu item when user has no data scope (DASHC-05)', () => {
    mockAcessoUsuario = null;
    renderLayout();

    expect(screen.getAllByText('Dashboard').length).toBeGreaterThan(0);
    expect(screen.queryByText('Meu Dashboard')).not.toBeInTheDocument();
  });

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

  it('shows Meu Workspace section with three sub-items for scoped users (WKS2-05)', () => {
    mockAcessoUsuario = {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: false,
      centrosCustoIds: [1],
      quantidadeCentrosAcessiveis: 1,
    };
    renderLayout('/workspace');

    expect(screen.getAllByText('Meu Workspace').length).toBeGreaterThan(0);
    expect(screen.queryByText('Workspace')).not.toBeInTheDocument();
    expect(screen.getAllByText('Meus workspaces').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Meus dados').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Catálogo de templates').length).toBeGreaterThan(0);
  });

  it('highlights active sub-nav item per workspace route (WKS2-06)', () => {
    mockAcessoUsuario = {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: false,
      centrosCustoIds: [1],
      quantidadeCentrosAcessiveis: 1,
    };

    const routes = [
      { path: '/workspace', label: 'Meus workspaces' },
      { path: '/workspace/datasets', label: 'Meus dados' },
      { path: '/workspace/templates', label: 'Catálogo de templates' },
    ] as const;

    for (const { path, label } of routes) {
      const { unmount } = renderLayout(path);
      const buttons = screen.getAllByRole('button', { name: label });
      expect(buttons.some((button) => button.classList.contains('Mui-selected'))).toBe(true);
      unmount();
    }
  });

  it('highlights Meus dados on dataset child routes (WKS2-07)', () => {
    mockAcessoUsuario = {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: false,
      centrosCustoIds: [1],
      quantidadeCentrosAcessiveis: 1,
    };
    renderLayout('/workspace/datasets/42');

    const datasetsButtons = screen.getAllByRole('button', { name: 'Meus dados' });
    expect(datasetsButtons.some((button) => button.classList.contains('Mui-selected'))).toBe(true);
  });

  it('highlights Meus workspaces on workspace detail routes (WKS2-07)', () => {
    mockAcessoUsuario = {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: false,
      centrosCustoIds: [1],
      quantidadeCentrosAcessiveis: 1,
    };
    renderLayout('/workspace/7');

    const hubButtons = screen.getAllByRole('button', { name: 'Meus workspaces' });
    expect(hubButtons.some((button) => button.classList.contains('Mui-selected'))).toBe(true);
  });
});
