import { test, expect } from '@playwright/test';

const API = '**/api';

function buildLoginResponse(login = 'e2e-user') {
  const now = Date.now();
  return {
    login,
    token: 'access-token-new',
    refreshToken: 'refresh-token-new',
    tokenExpiration: new Date(now + 60 * 60 * 1000).toISOString(),
    refreshExpiration: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
    acessoUsuario: {
      temFuncionarioVinculado: true,
      temNoOrganograma: true,
      acessoTotal: true,
      centrosCustoIds: [1],
      quantidadeCentrosAcessiveis: 1,
    },
  };
}

const mockStats = {
  totalFuncionarios: 42,
  custoMensalFolha: 125000.5,
  totalBeneficiosAtivos: 18,
  porLinhaNegocio: [],
  porCentroCusto: [],
  porCargo: [],
  totalProventos: 100000,
  totalDescontos: 25000,
  topProventos: [{ id: 1, codigo: '001', descricao: 'Salário', valorTotal: 80000, quantidadeOcorrencias: 40 }],
  topDescontos: [{ id: 2, codigo: '101', descricao: 'INSS', valorTotal: 15000, quantidadeOcorrencias: 40 }],
  evolucaoMensal: [{ mesAno: 'jun/2026', valorTotal: 125000, quantidadeFuncionarios: 42 }],
};

const defaultLayout = {
  id: 1,
  nome: 'Meu dashboard',
  widgets: [
    { widgetId: 'kpi-total-funcionarios', instanceId: 'a1', ordem: 0, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-custo-empresa', instanceId: 'a2', ordem: 1, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-beneficios-ativos', instanceId: 'a3', ordem: 2, colSpan: 3, rowSpan: 1 },
    { widgetId: 'kpi-relacao-pd', instanceId: 'a4', ordem: 3, colSpan: 3, rowSpan: 1 },
    { widgetId: 'grafico-evolucao-mensal', instanceId: 'a5', ordem: 4, colSpan: 12, rowSpan: 2 },
    { widgetId: 'grafico-funcionarios-por-cc', instanceId: 'a6', ordem: 5, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-funcionarios-por-linha', instanceId: 'a7', ordem: 6, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-custo-por-cc', instanceId: 'a8', ordem: 7, colSpan: 3, rowSpan: 2 },
    { widgetId: 'grafico-custo-por-linha', instanceId: 'a9', ordem: 8, colSpan: 3, rowSpan: 2 },
    { widgetId: 'lista-top-proventos', instanceId: 'a10', ordem: 9, colSpan: 6, rowSpan: 2 },
    { widgetId: 'lista-top-descontos', instanceId: 'a11', ordem: 10, colSpan: 6, rowSpan: 2 },
  ],
};

const catalog = [
  {
    widgetId: 'kpi-total-funcionarios',
    titulo: 'Total de Funcionários',
    descricao: 'KPI',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
  },
  {
    widgetId: 'kpi-beneficios-ativos',
    titulo: 'Benefícios Ativos',
    descricao: 'KPI',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
  },
];

async function mockAuthenticatedSession(page: import('@playwright/test').Page) {
  const loginResponse = buildLoginResponse('e2e-user');
  let layoutState = structuredClone(defaultLayout);

  await page.route(`${API}/auth/login`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(loginResponse),
    });
  });

  await page.route(`${API}/usuarios/login/${loginResponse.login}`, async (route) => {
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

  await page.route(`${API}/dashboard/layout`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(layoutState),
      });
      return;
    }
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON() as typeof defaultLayout;
      layoutState = { ...body, id: layoutState.id };
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(layoutState),
      });
      return;
    }
    await route.continue();
  });

  await page.route(`${API}/dashboard/widgets/catalog`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(catalog),
    });
  });

  await page.route(`${API}/dashboard/stats`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockStats),
    });
  });

  return {
    getLayout: () => layoutState,
  };
}

async function loginAndOpenMeuDashboard(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByRole('textbox', { name: /login/i }).fill('e2e-user');
  await page.getByLabel(/^Senha/).fill('secret');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await page.goto('/meu-dashboard');
  await expect(page.getByRole('heading', { name: 'Meu Dashboard' })).toBeVisible();
}

test.describe('Meu Dashboard layout persistence', () => {
  test('edit save reload keeps removed widget absent (DASHC-20)', async ({ page }) => {
    await mockAuthenticatedSession(page);
    await loginAndOpenMeuDashboard(page);

    await expect(page.getByLabel('Benefícios Ativos')).toBeVisible();
    await expect(page.getByLabel('Total de Funcionários')).toBeVisible();

    await page.getByRole('button', { name: 'Editar layout' }).click();
    await page.getByRole('button', { name: 'Remover Benefícios Ativos' }).click();
    await expect(page.getByLabel('Benefícios Ativos')).not.toBeVisible();

    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.getByRole('button', { name: 'Editar layout' })).toBeVisible();
    await expect(page.getByLabel('Benefícios Ativos')).not.toBeVisible();

    await page.reload();
    await expect(page.getByRole('heading', { name: 'Meu Dashboard' })).toBeVisible();
    await expect(page.getByLabel('Benefícios Ativos')).not.toBeVisible();
    await expect(page.getByLabel('Total de Funcionários')).toBeVisible();
  });
});
