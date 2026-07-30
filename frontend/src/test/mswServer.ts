import { setupServer, type SetupServerApi } from 'msw/node';
import type { HttpHandler } from 'msw';
import { authHandlers } from './handlers/authHandlers';

/**
 * Factory for an MSW server preloaded with default auth handlers
 * (`/auth/refresh` and a protected GET resource).
 *
 * ## Lifecycle (per test file — MSW is NOT wired globally in setup.ts)
 *
 * ```ts
 * const server = createAuthMswServer();
 *
 * beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
 * afterEach(() => server.resetHandlers());
 * afterAll(() => server.close());
 * ```
 *
 * - **listen**: start intercepting HTTP in Node (jsdom fetch + axios).
 * - **resetHandlers**: remove runtime overrides from `server.use()`; restore defaults.
 * - **close**: tear down the interceptor after the suite.
 *
 * Pass extra handlers to extend or replace defaults for a specific test file.
 */
export function createAuthMswServer(...extraHandlers: HttpHandler[]): SetupServerApi {
  return setupServer(...authHandlers, ...extraHandlers);
}
