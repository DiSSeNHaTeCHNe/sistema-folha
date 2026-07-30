import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import api, { getUserByLogin, login, logout, refreshToken, resetApiAuthState } from './api';
import { TokenService } from './tokenService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL, sampleLoginResponse } from '../test/handlers/authHandlers';
import type { TokenData } from '../types';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  TokenService.clearTokens();
  resetApiAuthState();
});
afterAll(() => server.close());

function setValidTokens(overrides: Partial<TokenData> = {}): void {
  const now = Date.now();
  TokenService.setTokens({
    token: 'old-access-token',
    refreshToken: 'old-refresh-token',
    tokenExpiration: new Date(now + 30 * 60 * 1000).toISOString(),
    refreshExpiration: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
    ...overrides,
  });
}

describe('api.ts auth interceptors', () => {
  it('retries a 401 protected request after refresh and sends the new Authorization header', async () => {
    setValidTokens();
    let protectedCallCount = 0;
    let lastAuthHeader: string | undefined;
    let refreshCallCount = 0;

    server.use(
      http.get(`${API_BASE_URL}/protected`, ({ request }) => {
        protectedCallCount += 1;
        lastAuthHeader = request.headers.get('Authorization') ?? undefined;
        if (protectedCallCount === 1) {
          return HttpResponse.json({ message: 'unauthorized' }, { status: 401 });
        }
        return HttpResponse.json({ ok: true });
      }),
      http.post(`${API_BASE_URL}/auth/refresh`, () => {
        refreshCallCount += 1;
        return HttpResponse.json(
          sampleLoginResponse({
            token: 'new-access-token',
            refreshToken: 'new-refresh-token',
          }),
        );
      }),
    );

    const response = await api.get('/protected');

    expect(response.status).toBe(200);
    expect(refreshCallCount).toBe(1);
    expect(protectedCallCount).toBe(2);
    expect(lastAuthHeader).toBe('Bearer new-access-token');
    expect(TokenService.getToken()).toBe('new-access-token');
  });

  it('clears tokens and dispatches auth:logout when refresh fails', async () => {
    setValidTokens();
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () => HttpResponse.json({}, { status: 401 })),
    );

    await expect(api.get('/protected')).rejects.toThrow('Falha ao renovar token');

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });

  it('queues parallel 401 requests during a single refresh and completes both retries', async () => {
    setValidTokens();
    const protectedAttempts = new Map<string, number>();
    let refreshCallCount = 0;

    server.use(
      http.get(`${API_BASE_URL}/protected`, ({ request }) => {
        const id = new URL(request.url).searchParams.get('id') ?? 'default';
        const attempt = (protectedAttempts.get(id) ?? 0) + 1;
        protectedAttempts.set(id, attempt);
        if (attempt === 1) {
          return HttpResponse.json({}, { status: 401 });
        }
        return HttpResponse.json({ id });
      }),
      http.post(`${API_BASE_URL}/auth/refresh`, () => {
        refreshCallCount += 1;
        return HttpResponse.json(sampleLoginResponse({ token: 'queued-token' }));
      }),
    );

    const [first, second] = await Promise.all([
      api.get('/protected', { params: { id: 'a' } }),
      api.get('/protected', { params: { id: 'b' } }),
    ]);

    expect(refreshCallCount).toBe(1);
    expect(first.data).toEqual({ id: 'a' });
    expect(second.data).toEqual({ id: 'b' });
  });

  it('logs out and clears tokens when POST /auth/refresh returns 401', async () => {
    setValidTokens();
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.post(`${API_BASE_URL}/auth/refresh`, () => HttpResponse.json({}, { status: 401 })),
    );

    await expect(
      api.post('/auth/refresh', { refreshToken: 'old-refresh-token' }),
    ).rejects.toMatchObject({ response: { status: 401 } });

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });

  it('treats 403 as unauthorized and refreshes before retrying', async () => {
    setValidTokens();
    let protectedCallCount = 0;

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => {
        protectedCallCount += 1;
        if (protectedCallCount === 1) {
          return HttpResponse.json({}, { status: 403 });
        }
        return HttpResponse.json({ ok: true });
      }),
      http.post(`${API_BASE_URL}/auth/refresh`, () =>
        HttpResponse.json(sampleLoginResponse({ token: 'forbidden-retry-token' })),
      ),
    );

    const response = await api.get('/protected');

    expect(response.status).toBe(200);
    expect(protectedCallCount).toBe(2);
    expect(TokenService.getToken()).toBe('forbidden-retry-token');
  });

  it('logs out without calling refresh when the refresh token is locally expired', async () => {
    setValidTokens({
      refreshExpiration: new Date(Date.now() - 60_000).toISOString(),
    });
    let refreshCallCount = 0;
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () => {
        refreshCallCount += 1;
        return HttpResponse.json(sampleLoginResponse());
      }),
    );

    await expect(api.get('/protected')).rejects.toThrow('Refresh token expirado');

    expect(refreshCallCount).toBe(0);
    expect(TokenService.getToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });

  it('clears stored tokens after logout even when the server request fails', async () => {
    setValidTokens();

    server.use(
      http.post(`${API_BASE_URL}/auth/logout`, () => HttpResponse.error()),
    );

    await logout();

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
  });

  it('clears stored tokens after a successful server logout', async () => {
    setValidTokens();

    server.use(
      http.post(`${API_BASE_URL}/auth/logout`, () => HttpResponse.json({})),
    );

    await logout();

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
  });

  it('adds Authorization header to outgoing requests when a token is stored', async () => {
    setValidTokens({ token: 'stored-token' });
    let authHeader: string | null = null;

    server.use(
      http.get(`${API_BASE_URL}/protected`, ({ request }) => {
        authHeader = request.headers.get('Authorization');
        return HttpResponse.json({ ok: true });
      }),
    );

    await api.get('/protected');

    expect(authHeader).toBe('Bearer stored-token');
  });

  it('logs out when refresh is attempted without a refresh token in storage', async () => {
    localStorage.setItem('token', 'access-only');
    localStorage.setItem(
      'tokenExpiration',
      new Date(Date.now() + 30 * 60 * 1000).toISOString(),
    );
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);
    let refreshCallCount = 0;

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () => {
        refreshCallCount += 1;
        return HttpResponse.json(sampleLoginResponse());
      }),
    );

    await expect(api.get('/protected')).rejects.toThrow('Refresh token não disponível');

    expect(refreshCallCount).toBe(0);
    expect(TokenService.getToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });

  it('login posts credentials and returns the LoginResponse payload', async () => {
    const loginResponse = sampleLoginResponse({ login: 'admin', token: 'login-token' });

    server.use(
      http.post(`${API_BASE_URL}/auth/login`, async ({ request }) => {
        const body = await request.json() as { login: string; senha: string };
        expect(body).toEqual({ login: 'admin', senha: 'secret' });
        return HttpResponse.json(loginResponse);
      }),
    );

    const result = await login({ login: 'admin', senha: 'secret' });

    expect(result).toEqual(loginResponse);
  });

  it('refreshToken posts the refresh token and returns a LoginResponse payload', async () => {
    const refreshed = sampleLoginResponse({ token: 'refreshed-via-export' });

    server.use(
      http.post(`${API_BASE_URL}/auth/refresh`, async ({ request }) => {
        const body = await request.json() as { refreshToken: string };
        expect(body.refreshToken).toBe('refresh-me');
        return HttpResponse.json(refreshed);
      }),
    );

    const result = await refreshToken('refresh-me');

    expect(result).toEqual(refreshed);
  });

  it('getUserByLogin fetches the user resource by login', async () => {
    server.use(
      http.get(`${API_BASE_URL}/usuarios/login/admin`, () =>
        HttpResponse.json({ id: 1, login: 'admin', nome: 'Admin', permissoes: ['ADMIN'] }),
      ),
    );

    const user = await getUserByLogin('admin');

    expect(user).toEqual({ id: 1, login: 'admin', nome: 'Admin', permissoes: ['ADMIN'] });
  });

  it('logout clears tokens locally when no refresh token is stored', async () => {
    localStorage.setItem('token', 'orphan-access');

    await logout();

    expect(TokenService.getToken()).toBeNull();
  });

  it('does not attach Authorization when no access token is stored', async () => {
    let authHeader: string | null = 'unset';

    server.use(
      http.get(`${API_BASE_URL}/protected`, ({ request }) => {
        authHeader = request.headers.get('Authorization');
        return HttpResponse.json({ ok: true });
      }),
    );

    await api.get('/protected');

    expect(authHeader).toBeNull();
  });

  it('rejects queued requests when refresh fails during concurrent 401 handling', async () => {
    setValidTokens();
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => HttpResponse.json({}, { status: 401 })),
      http.get(`${API_BASE_URL}/protected-other`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () =>
        HttpResponse.json({ message: 'invalid refresh' }, { status: 401 }),
      ),
    );

    await expect(
      Promise.all([api.get('/protected'), api.get('/protected-other')]),
    ).rejects.toThrow('Falha ao renovar token');

    expect(TokenService.getToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);
    window.removeEventListener('auth:logout', logoutListener);
  });

  it('logout keeps clearing tokens when the server logout call fails', async () => {
    setValidTokens();
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    server.use(
      http.post(`${API_BASE_URL}/auth/logout`, () =>
        HttpResponse.json({ message: 'server down' }, { status: 500 }),
      ),
    );

    await logout();

    expect(TokenService.getToken()).toBeNull();
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it('logs out when refresh returns 500 during a 401 retry', async () => {
    setValidTokens();
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.get(`${API_BASE_URL}/protected`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () =>
        HttpResponse.json({ message: 'server error' }, { status: 500 }),
      ),
    );

    await expect(api.get('/protected')).rejects.toThrow('Falha ao renovar token');

    expect(TokenService.getToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });

  it('logs out when refresh endpoint returns 401 on a direct refreshToken call', async () => {
    setValidTokens();
    const logoutListener = vi.fn();
    window.addEventListener('auth:logout', logoutListener);

    server.use(
      http.post(`${API_BASE_URL}/auth/refresh`, () =>
        HttpResponse.json({ message: 'invalid' }, { status: 401 }),
      ),
    );

    await expect(refreshToken('stored-refresh')).rejects.toMatchObject({ response: { status: 401 } });
    expect(TokenService.getToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });
});
