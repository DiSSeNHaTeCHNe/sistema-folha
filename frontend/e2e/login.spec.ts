import { test, expect } from '@playwright/test';

/** Same shape as sampleLoginResponse() in authHandlers.ts — inlined for Playwright Node context. */
function buildLoginResponse(login = 'e2e-user') {
  const now = Date.now();
  return {
    login,
    token: 'access-token-new',
    refreshToken: 'refresh-token-new',
    tokenExpiration: new Date(now + 60 * 60 * 1000).toISOString(),
    refreshExpiration: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
  };
}

test.describe('Login page', () => {
  test('shows heading and submits with mocked auth login', async ({ page }) => {
    const loginResponse = buildLoginResponse('e2e-user');

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(loginResponse),
      });
    });

    await page.route(`**/usuarios/login/${loginResponse.login}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          login: loginResponse.login,
          nome: 'E2E User',
          permissoes: ['USER'],
        }),
      });
    });

    await page.goto('/login');

    await expect(page.getByRole('heading', { name: 'Sistema de Folha' })).toBeVisible();

    await page.getByRole('textbox', { name: /login/i }).fill('e2e-user');
    await page.getByLabel(/^Senha/).fill('secret');
    await page.getByRole('button', { name: 'Entrar' }).click();

    await expect(page).toHaveURL(/\/dashboard/);
  });
});
