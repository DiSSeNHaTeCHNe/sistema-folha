import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { AuthProvider, useAuth } from './AuthContext';
import { TokenService } from '../services/tokenService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL, sampleLoginResponse } from '../test/handlers/authHandlers';
import type { AcessoUsuario, Usuario } from '../types';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  TokenService.clearTokens();
  localStorage.clear();
});
afterAll(() => server.close());

function setValidTokens(): void {
  const now = Date.now();
  TokenService.setTokens({
    token: 'stored-access',
    refreshToken: 'stored-refresh',
    tokenExpiration: new Date(now + 30 * 60 * 1000).toISOString(),
    refreshExpiration: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
  });
}

function AuthProbe() {
  const { user, loading, isAuthenticated, podeAcessarCentroCusto, login, logout } = useAuth();

  if (loading) {
    return <div role="status">loading</div>;
  }

  return (
    <div>
      <div>authenticated:{String(isAuthenticated)}</div>
      <div>user:{user?.login ?? 'none'}</div>
      <div>can-access-1:{String(podeAcessarCentroCusto(1))}</div>
      <button type="button" onClick={() => login({ login: 'admin', senha: 'secret' })}>
        do-login
      </button>
      <button type="button" onClick={() => logout()}>do-logout</button>
    </div>
  );
}

function renderAuthProbe(initialRoute = '/') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    </MemoryRouter>,
  );
}

const sampleUser: Usuario = {
  id: 1,
  login: 'stored',
  nome: 'Stored User',
  permissoes: ['ADMIN'],
};

const sampleAcesso: AcessoUsuario = {
  temFuncionarioVinculado: true,
  temNoOrganograma: true,
  acessoTotal: false,
  centrosCustoIds: [1, 2],
  quantidadeCentrosAcessiveis: 2,
};

describe('AuthContext', () => {
  it('restores a stored user when tokens remain valid', async () => {
    setValidTokens();
    localStorage.setItem('user', JSON.stringify(sampleUser));
    localStorage.setItem('acessoUsuario', JSON.stringify(sampleAcesso));

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('user:stored')).toBeInTheDocument();
    });
    expect(screen.getByText('authenticated:true')).toBeInTheDocument();
    expect(screen.getByText('can-access-1:true')).toBeInTheDocument();
  });

  it('clears auth state when tokens are missing on initialization', async () => {
    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('authenticated:false')).toBeInTheDocument();
    });
    expect(screen.getByText('user:none')).toBeInTheDocument();
  });

  it('stores tokens and user data after a successful login', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/login`, () =>
        HttpResponse.json(
          sampleLoginResponse({
            login: 'admin',
            acessoUsuario: { ...sampleAcesso, acessoTotal: true },
          }),
        ),
      ),
      http.get(`${API_BASE_URL}/usuarios/login/admin`, () =>
        HttpResponse.json({ id: 1, login: 'admin', nome: 'Admin', permissoes: ['ADMIN'] }),
      ),
    );

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'do-login' }));

    await waitFor(() => {
      expect(screen.getByText('user:admin')).toBeInTheDocument();
    });
    expect(TokenService.getToken()).toBe('access-token-new');
    expect(localStorage.getItem('user')).toContain('"login":"admin"');
  });

  it('grants global centro de custo access when acessoTotal is true', async () => {
    setValidTokens();
    localStorage.setItem('user', JSON.stringify(sampleUser));
    localStorage.setItem(
      'acessoUsuario',
      JSON.stringify({ ...sampleAcesso, acessoTotal: true, centrosCustoIds: [] }),
    );

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('can-access-1:true')).toBeInTheDocument();
    });
  });

  it('denies centro de custo access when the user lacks organogram linkage', async () => {
    setValidTokens();
    localStorage.setItem('user', JSON.stringify(sampleUser));
    localStorage.setItem(
      'acessoUsuario',
      JSON.stringify({ ...sampleAcesso, temNoOrganograma: false }),
    );

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('can-access-1:false')).toBeInTheDocument();
    });
  });

  it('clears auth state after logout', async () => {
    setValidTokens();
    localStorage.setItem('user', JSON.stringify(sampleUser));

    server.use(
      http.post(`${API_BASE_URL}/auth/logout`, () => HttpResponse.json({})),
    );

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('user:stored')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'do-logout' }));

    await waitFor(() => {
      expect(screen.getByText('user:none')).toBeInTheDocument();
    });
    expect(TokenService.getToken()).toBeNull();
  });

  it('handles auth:logout window event by clearing session', async () => {
    setValidTokens();
    localStorage.setItem('user', JSON.stringify(sampleUser));

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('user:stored')).toBeInTheDocument();
    });

    window.dispatchEvent(new CustomEvent('auth:logout'));

    await waitFor(() => {
      expect(screen.getByText('user:none')).toBeInTheDocument();
    });
  });

  it('clears auth when stored user JSON is invalid', async () => {
    setValidTokens();
    localStorage.setItem('user', '{invalid-json');

    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByText('user:none')).toBeInTheDocument();
    });
  });
});
