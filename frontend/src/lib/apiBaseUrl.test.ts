import { afterEach, describe, expect, it, vi } from 'vitest';

describe('apiBaseUrl', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('defaults to http://localhost:8083/api when VITE_API_URL is unset', async () => {
    vi.stubEnv('VITE_API_URL', '');
    const { getApiBaseUrl, API_BASE_URL } = await import('./apiBaseUrl');

    expect(getApiBaseUrl()).toBe('http://localhost:8083/api');
    expect(API_BASE_URL).toBe('http://localhost:8083/api');
  });

  it('uses VITE_API_URL override and keeps MSW handlers on the same base', async () => {
    const customBase = 'http://custom-host:9000/api';
    vi.stubEnv('VITE_API_URL', customBase);

    const { getApiBaseUrl } = await import('./apiBaseUrl');
    const { API_BASE_URL } = await import('../test/handlers/authHandlers');

    expect(getApiBaseUrl()).toBe(customBase);
    expect(API_BASE_URL).toBe(customBase);
  });
});
