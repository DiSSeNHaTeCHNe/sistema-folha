import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { createAuthMswServer } from './mswServer';
import { API_BASE_URL, sampleLoginResponse } from './handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('auth MSW harness', () => {
  it('sampleLoginResponse merges overrides into the default payload', () => {
    const response = sampleLoginResponse({ login: 'operator', token: 'custom-token' });

    expect(response.login).toBe('operator');
    expect(response.token).toBe('custom-token');
    expect(response.refreshToken).toBeTruthy();
  });

  it('default refresh handler returns LoginResponse shape', async () => {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, { method: 'POST' });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.token).toBeTruthy();
    expect(body.refreshToken).toBeTruthy();
  });

  it('default protected handler returns ok payload', async () => {
    const response = await fetch(`${API_BASE_URL}/protected`);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({ ok: true });
  });
});
