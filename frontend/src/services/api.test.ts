import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import api, { resetApiAuthState } from './api';
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

    await expect(api.get('/protected')).rejects.toBeDefined();

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
    ).rejects.toBeDefined();

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
    expect(logoutListener).toHaveBeenCalledTimes(1);

    window.removeEventListener('auth:logout', logoutListener);
  });
});
