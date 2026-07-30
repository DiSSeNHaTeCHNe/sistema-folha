import { http, HttpResponse, type HttpHandler } from 'msw';
import type { LoginResponse } from '../../types';

export const API_BASE_URL = 'http://localhost:8083/api';

export function sampleLoginResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
  const now = Date.now();
  return {
    login: 'test-user',
    token: 'access-token-new',
    refreshToken: 'refresh-token-new',
    tokenExpiration: new Date(now + 60 * 60 * 1000).toISOString(),
    refreshExpiration: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
    ...overrides,
  };
}

/** Default POST /auth/refresh — returns LoginResponse shape. Override per test via server.use(). */
export const authRefreshHandler: HttpHandler = http.post(
  `${API_BASE_URL}/auth/refresh`,
  async () => HttpResponse.json(sampleLoginResponse()),
);

/** Default protected resource — returns 200. Override to simulate 401 for interceptor tests. */
export const protectedResourceHandler: HttpHandler = http.get(
  `${API_BASE_URL}/protected`,
  () => HttpResponse.json({ ok: true }),
);

export const authHandlers: HttpHandler[] = [authRefreshHandler, protectedResourceHandler];
