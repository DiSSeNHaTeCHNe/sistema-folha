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

interface MockDataset {
  id: number;
  nome: string;
  campos: { nome: string; tipo: string }[];
  schemaVersion: number;
  totalLinhas: number;
}

interface MockRow {
  id: number;
  datasetId: number;
  valores: Record<string, unknown>;
  ordem: number;
}

interface MockWidgetDef {
  id: number;
  nome: string;
  tipo: string;
  fontes: { kind: string; ref: string }[];
  formula: string | null;
  config: Record<string, unknown>;
  invalido: boolean;
}

interface MockWorkspace {
  id: number;
  nome: string;
  widgets: {
    instanceId: string;
    ordem: number;
    colSpan: number;
    rowSpan: number;
    widgetId?: string | null;
    userWidgetDefinitionId?: number | null;
    config?: Record<string, unknown> | null;
  }[];
}

function createWorkspaceMockState() {
  let nextDatasetId = 1;
  let nextRowId = 1;
  let nextWidgetId = 1;
  let nextWorkspaceId = 1;
  let nextInstanceCounter = 1;

  const datasets: MockDataset[] = [];
  const rowsByDataset = new Map<number, MockRow[]>();
  const widgetDefinitions: MockWidgetDef[] = [];
  const workspaces: MockWorkspace[] = [];

  const datasetSummary = (dataset: MockDataset) => ({
    id: dataset.id,
    nome: dataset.nome,
    schemaVersion: dataset.schemaVersion,
    totalLinhas: dataset.totalLinhas,
    totalCampos: dataset.campos.length,
  });

  const workspaceSummary = (workspace: MockWorkspace) => ({
    id: workspace.id,
    nome: workspace.nome,
    totalWidgets: workspace.widgets.length,
  });

  const findWorkspace = (id: number) => workspaces.find((item) => item.id === id);

  const installOrcamento = (workspaceId: number) => {
    let dataset = datasets.find((item) => item.nome === 'Orçamento por CC');
    if (!dataset) {
      dataset = {
        id: nextDatasetId++,
        nome: 'Orçamento por CC',
        campos: [
          { nome: 'competencia', tipo: 'DATA' },
          { nome: 'centro_custo_id', tipo: 'REFERENCIA' },
          { nome: 'valor_orcado', tipo: 'MOEDA' },
        ],
        schemaVersion: 1,
        totalLinhas: 0,
      };
      datasets.push(dataset);
      rowsByDataset.set(dataset.id, []);
    }

    let tabela = widgetDefinitions.find((item) => item.nome === 'Orçado x Realizado');
    if (!tabela) {
      tabela = {
        id: nextWidgetId++,
        nome: 'Orçado x Realizado',
        tipo: 'TABELA',
        fontes: [
          { kind: 'DATASET', ref: String(dataset.id) },
          { kind: 'SISTEMA', ref: 'ORCAMENTO' },
        ],
        formula: null,
        config: { colunas: ['centro_custo', 'orcado', 'realizado', 'variacao'] },
        invalido: false,
      };
      widgetDefinitions.push(tabela);
    }

    let kpi = widgetDefinitions.find((item) => item.nome === 'Variação %');
    if (!kpi) {
      kpi = {
        id: nextWidgetId++,
        nome: 'Variação %',
        tipo: 'KPI',
        fontes: [
          { kind: 'DATASET', ref: String(dataset.id) },
          { kind: 'SISTEMA', ref: 'ORCAMENTO' },
        ],
        formula: 'SE(MÉDIA(realizado)=0, 0, (SOMA(valor_orcado)-SOMA(realizado))/SOMA(realizado)*100)',
        config: {},
        invalido: false,
      };
      widgetDefinitions.push(kpi);
    }

    const workspace = findWorkspace(workspaceId);
    if (workspace) {
      workspace.widgets = [
        {
          instanceId: `orc-${nextInstanceCounter++}`,
          ordem: 0,
          colSpan: 12,
          rowSpan: 2,
          userWidgetDefinitionId: tabela.id,
        },
        {
          instanceId: `orc-${nextInstanceCounter++}`,
          ordem: 1,
          colSpan: 4,
          rowSpan: 1,
          userWidgetDefinitionId: kpi.id,
        },
      ];
    }

    return {
      workspaceId,
      datasetId: dataset.id,
      widgetDefinitionIds: [tabela.id, kpi.id],
      widgetsAdicionados: 2,
    };
  };

  return {
    datasets,
    rowsByDataset,
    widgetDefinitions,
    workspaces,
    datasetSummary,
    workspaceSummary,
    findWorkspace,
    installOrcamento,
    allocDataset: () => nextDatasetId++,
    allocRow: () => nextRowId++,
    allocWidget: () => nextWidgetId++,
    allocWorkspace: () => nextWorkspaceId++,
    allocInstance: () => `w-${nextInstanceCounter++}`,
  };
}

async function mockWorkspaceSession(page: import('@playwright/test').Page) {
  const loginResponse = buildLoginResponse('e2e-user');
  const state = createWorkspaceMockState();

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

  await page.route(`${API}/workspace/datasets**`, async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    const matchRows = url.pathname.match(/\/workspace\/datasets\/(\d+)\/rows(?:\/(\d+))?$/);
    const matchDataset = url.pathname.match(/\/workspace\/datasets\/(\d+)$/);
    const isCollection = url.pathname.endsWith('/workspace/datasets');

    if (isCollection && method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state.datasets.map(state.datasetSummary)),
      });
      return;
    }

    if (isCollection && method === 'POST') {
      const body = route.request().postDataJSON() as { nome: string; campos: MockDataset['campos'] };
      const dataset: MockDataset = {
        id: state.allocDataset(),
        nome: body.nome,
        campos: body.campos,
        schemaVersion: 1,
        totalLinhas: 0,
      };
      state.datasets.push(dataset);
      state.rowsByDataset.set(dataset.id, []);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...dataset, totalLinhas: 0 }),
      });
      return;
    }

    if (matchRows && method === 'GET') {
      const datasetId = Number(matchRows[1]);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state.rowsByDataset.get(datasetId) ?? []),
      });
      return;
    }

    if (matchRows && method === 'POST') {
      const datasetId = Number(matchRows[1]);
      const body = route.request().postDataJSON() as { valores: Record<string, unknown> };
      const row: MockRow = {
        id: state.allocRow(),
        datasetId,
        valores: body.valores,
        ordem: state.rowsByDataset.get(datasetId)?.length ?? 0,
      };
      state.rowsByDataset.get(datasetId)?.push(row);
      const dataset = state.datasets.find((item) => item.id === datasetId);
      if (dataset) {
        dataset.totalLinhas += 1;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(row) });
      return;
    }

    if (matchRows && method === 'PUT') {
      const datasetId = Number(matchRows[1]);
      const rowId = Number(matchRows[2]);
      const body = route.request().postDataJSON() as { valores: Record<string, unknown> };
      const rows = state.rowsByDataset.get(datasetId) ?? [];
      const row = rows.find((item) => item.id === rowId);
      if (row) {
        row.valores = body.valores;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(row) });
      return;
    }

    if (matchDataset && method === 'GET') {
      const datasetId = Number(matchDataset[1]);
      const dataset = state.datasets.find((item) => item.id === datasetId);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(dataset ?? null),
      });
      return;
    }

    await route.continue();
  });

  await page.route(`${API}/workspace/widget-definitions**`, async (route) => {
    const method = route.request().method();
    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state.widgetDefinitions),
      });
      return;
    }
    if (method === 'POST') {
      const body = route.request().postDataJSON() as Omit<MockWidgetDef, 'id' | 'invalido'>;
      const created: MockWidgetDef = {
        id: state.allocWidget(),
        invalido: false,
        ...body,
      };
      state.widgetDefinitions.push(created);
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(created) });
      return;
    }
    await route.continue();
  });

  await page.route(`${API}/workspace/workspaces**`, async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    const layoutMatch = url.pathname.match(/\/workspace\/workspaces\/(\d+)\/layout$/);
    const dataMatch = url.pathname.match(/\/workspace\/workspaces\/(\d+)\/widgets\/([^/]+)\/data$/);
    const detailMatch = url.pathname.match(/\/workspace\/workspaces\/(\d+)$/);
    const isCollection = url.pathname.endsWith('/workspace/workspaces');

    if (isCollection && method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state.workspaces.map(state.workspaceSummary)),
      });
      return;
    }

    if (isCollection && method === 'POST') {
      const body = route.request().postDataJSON() as { nome: string };
      const workspace: MockWorkspace = { id: state.allocWorkspace(), nome: body.nome, widgets: [] };
      state.workspaces.push(workspace);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(workspace),
      });
      return;
    }

    if (layoutMatch && method === 'PUT') {
      const workspaceId = Number(layoutMatch[1]);
      const body = route.request().postDataJSON() as { widgets: MockWorkspace['widgets'] };
      const workspace = state.findWorkspace(workspaceId);
      if (workspace) {
        workspace.widgets = body.widgets;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(workspace),
      });
      return;
    }

    if (dataMatch && method === 'GET') {
      const workspaceId = Number(dataMatch[1]);
      const instanceId = dataMatch[2];
      const workspace = state.findWorkspace(workspaceId);
      const widget = workspace?.widgets.find((item) => item.instanceId === instanceId);
      const userDef = state.widgetDefinitions.find((item) => item.id === widget?.userWidgetDefinitionId);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          instanceId,
          userWidgetDefinitionId: widget?.userWidgetDefinitionId ?? null,
          widgetId: widget?.widgetId ?? null,
          tipo: userDef?.tipo ?? 'KPI',
          semDados: false,
          invalido: false,
          competencia: null,
          valores: userDef?.tipo === 'KPI' ? { valor: '12,5' } : {},
          linhas:
            userDef?.tipo === 'TABELA'
              ? [{ centro_custo: 'CC-01', orcado: '10000,00', realizado: '8500,00', variacao: '-15,0%' }]
              : [],
        }),
      });
      return;
    }

    if (detailMatch && method === 'GET') {
      const workspaceId = Number(detailMatch[1]);
      const workspace = state.findWorkspace(workspaceId);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(workspace ?? null),
      });
      return;
    }

    if (detailMatch && method === 'DELETE') {
      const workspaceId = Number(detailMatch[1]);
      const index = state.workspaces.findIndex((item) => item.id === workspaceId);
      if (index >= 0) {
        state.workspaces.splice(index, 1);
      }
      await route.fulfill({ status: 204, body: '' });
      return;
    }

    await route.continue();
  });

  await page.route(`${API}/workspace/templates/orcamento-padrao/install`, async (route) => {
    const body = route.request().postDataJSON() as { workspaceId: number };
    const result = state.installOrcamento(body.workspaceId);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(result),
    });
  });

  return { state, loginResponse };
}

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByRole('textbox', { name: /login/i }).fill('e2e-user');
  await page.getByLabel(/^Senha/).fill('secret');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
}

test.describe('Workspace P1 smoke', () => {
  test('create dataset, row, widget, two workspaces, install orçamento template', async ({ page }) => {
    await mockWorkspaceSession(page);
    await login(page);

    await page.goto('/workspace/datasets');
    await expect(page.getByRole('heading', { name: 'Datasets' })).toBeVisible();

    await page.getByRole('button', { name: 'Novo dataset' }).click();
    await page.getByLabel('Nome do dataset').fill('Previsão E2E');
    await page.getByRole('button', { name: 'Criar' }).click();

    await expect(page.getByRole('heading', { name: /Editor: Previsão E2E/i })).toBeVisible();
    await page.getByRole('button', { name: 'Adicionar linha' }).click();
    await page.getByRole('textbox', { name: 'valor' }).fill('42');
    await page.getByRole('button', { name: 'Salvar linha' }).click();

    await page.getByRole('link', { name: 'Voltar aos datasets' }).click();
    await expect(page.getByRole('link', { name: 'Previsão E2E' })).toBeVisible();

    await page.getByRole('link', { name: 'Voltar ao workspace' }).click();
    await expect(page.getByRole('heading', { name: 'Workspace', exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Criar workspace' }).click();
    await page.getByRole('textbox', { name: 'Nome do workspace' }).fill('Planejamento');
    await page.getByRole('button', { name: 'Criar' }).click();
    await expect(page.getByRole('combobox', { name: 'Workspace' })).toBeVisible();

    await page.getByRole('button', { name: 'Novo widget' }).click();
    await page.getByLabel('Nome').fill('KPI Previsão');
    await page.getByLabel('Fonte (dataset id ou SISTEMA:ORCAMENTO)').fill('1');
    await page.getByRole('button', { name: 'Salvar' }).click();

    await page.getByRole('button', { name: 'Novo workspace' }).click();
    await page.getByRole('textbox', { name: 'Nome do workspace' }).fill('Operacional');
    await page.getByRole('button', { name: 'Criar' }).click();
    await expect(page.getByRole('dialog', { name: 'Novo workspace' })).not.toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Workspace' })).toHaveText('Operacional');

    await page.getByRole('button', { name: 'Instalar template de orçamento' }).click();
    await expect(page.getByText('Template de orçamento instalado')).toBeVisible();
    await expect(page.getByRole('table', { name: 'Tabela Orçado x Realizado' })).toBeVisible();
    await expect(page.getByLabel('Variação %', { exact: true })).toBeVisible();
  });
});
