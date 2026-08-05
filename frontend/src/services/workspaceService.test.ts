import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from './api';
import {
  WorkspaceApiError,
  createDataset,
  createDatasetRow,
  createWorkspace,
  createWidgetDefinition,
  deleteDataset,
  getDataset,
  getWorkspace,
  getWorkspaceWidgetData,
  listDatasets,
  listWorkspaces,
  listWidgetDefinitions,
  saveWorkspaceLayout,
  updateDatasetSchema,
} from './workspaceService';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockedApi = vi.mocked(api);

describe('workspaceService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('listDatasets returns dataset summaries', async () => {
    const summaries = [{ id: 1, nome: 'Previsão', schemaVersion: 1, totalLinhas: 0, totalCampos: 2 }];
    mockedApi.get.mockResolvedValue({ data: summaries });

    await expect(listDatasets()).resolves.toEqual(summaries);
    expect(mockedApi.get).toHaveBeenCalledWith('/workspace/datasets');
  });

  it('createDataset posts nome and campos', async () => {
    const dataset = {
      id: 2,
      nome: 'Headcount',
      campos: [{ nome: 'qtd', tipo: 'NUMERO' as const }],
      schemaVersion: 1,
      totalLinhas: 0,
    };
    mockedApi.post.mockResolvedValue({ data: dataset });

    await expect(
      createDataset('Headcount', [{ nome: 'qtd', tipo: 'NUMERO' }]),
    ).resolves.toEqual(dataset);
    expect(mockedApi.post).toHaveBeenCalledWith('/workspace/datasets', {
      nome: 'Headcount',
      campos: [{ nome: 'qtd', tipo: 'NUMERO' }],
    });
  });

  it('updateDatasetSchema sends schemaVersion and confirmarRemocao', async () => {
    const updated = {
      id: 1,
      nome: 'Previsão',
      campos: [{ nome: 'valor', tipo: 'MOEDA' as const }],
      schemaVersion: 2,
      totalLinhas: 1,
    };
    mockedApi.put.mockResolvedValue({ data: updated });

    await expect(
      updateDatasetSchema(1, [{ nome: 'valor', tipo: 'MOEDA' }], 1, true),
    ).resolves.toEqual(updated);
    expect(mockedApi.put).toHaveBeenCalledWith('/workspace/datasets/1/schema', {
      campos: [{ nome: 'valor', tipo: 'MOEDA' }],
      schemaVersion: 1,
      confirmarRemocao: true,
    });
  });

  it('createDatasetRow posts valores map', async () => {
    const row = { id: 10, datasetId: 1, valores: { qtd: 5 }, ordem: 0 };
    mockedApi.post.mockResolvedValue({ data: row });

    await expect(createDatasetRow(1, { qtd: 5 })).resolves.toEqual(row);
    expect(mockedApi.post).toHaveBeenCalledWith('/workspace/datasets/1/rows', { valores: { qtd: 5 } });
  });

  it('listWidgetDefinitions fetches user widgets', async () => {
    const widgets = [
      {
        id: 3,
        nome: 'KPI Total',
        tipo: 'KPI' as const,
        fontes: [{ kind: 'DATASET' as const, ref: '1' }],
        formula: 'SOMA(valor)',
        config: {},
        invalido: false,
      },
    ];
    mockedApi.get.mockResolvedValue({ data: widgets });

    await expect(listWidgetDefinitions()).resolves.toEqual(widgets);
  });

  it('createWidgetDefinition posts definition payload', async () => {
    const payload = {
      nome: 'Tabela CC',
      tipo: 'TABELA' as const,
      fontes: [{ kind: 'SISTEMA' as const, ref: 'ORCAMENTO' }],
      formula: null,
      config: {},
    };
    const created = { id: 4, ...payload, invalido: false };
    mockedApi.post.mockResolvedValue({ data: created });

    await expect(createWidgetDefinition(payload)).resolves.toEqual(created);
  });

  it('listWorkspaces and saveWorkspaceLayout use workspace endpoints', async () => {
    mockedApi.get.mockResolvedValue({ data: [{ id: 5, nome: 'Planejamento', totalWidgets: 2 }] });
    await expect(listWorkspaces()).resolves.toHaveLength(1);

    const workspace = {
      id: 5,
      nome: 'Planejamento',
      widgets: [{ instanceId: 'w1', ordem: 0, colSpan: 4, rowSpan: 1, widgetId: 'kpi-total-funcionarios' }],
    };
    mockedApi.put.mockResolvedValue({ data: workspace });
    await expect(saveWorkspaceLayout(5, workspace.widgets)).resolves.toEqual(workspace);
    expect(mockedApi.put).toHaveBeenCalledWith('/workspace/workspaces/5/layout', { widgets: workspace.widgets });
  });

  it('getWorkspaceWidgetData passes competencia query param', async () => {
    const data = {
      instanceId: 'w1',
      userWidgetDefinitionId: 1,
      widgetId: null,
      tipo: 'KPI',
      semDados: false,
      invalido: false,
      competencia: '2026-06',
      valores: { total: 'R$ 1.234,56' },
      linhas: [],
    };
    mockedApi.get.mockResolvedValue({ data });

    await expect(getWorkspaceWidgetData(5, 'w1', '2026-06')).resolves.toEqual(data);
    expect(mockedApi.get).toHaveBeenCalledWith('/workspace/workspaces/5/widgets/w1/data', {
      params: { competencia: '2026-06' },
    });
  });

  it('throws WorkspaceApiError with field errors on validation failure', async () => {
    const axiosError = {
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          status: 400,
          message: 'Validação falhou',
          errors: [{ field: 'valores.qtd', message: 'Esperado número' }],
        },
      },
    };
    mockedApi.post.mockRejectedValue(axiosError);

    await expect(createDatasetRow(1, { qtd: 'texto' })).rejects.toBeInstanceOf(WorkspaceApiError);
    await expect(createDatasetRow(1, { qtd: 'texto' })).rejects.toMatchObject({
      status: 400,
      message: 'Validação falhou',
      errors: [{ field: 'valores.qtd', message: 'Esperado número' }],
    });
  });

  it('getDataset and deleteDataset call correct paths', async () => {
    mockedApi.get.mockResolvedValue({
      data: { id: 1, nome: 'X', campos: [], schemaVersion: 1, totalLinhas: 0 },
    });
    await getDataset(1);
    expect(mockedApi.get).toHaveBeenCalledWith('/workspace/datasets/1');

    mockedApi.delete.mockResolvedValue({ data: undefined });
    await deleteDataset(1);
    expect(mockedApi.delete).toHaveBeenCalledWith('/workspace/datasets/1');
  });

  it('createWorkspace posts nome', async () => {
    const ws = { id: 7, nome: 'Trimestral', widgets: [] };
    mockedApi.post.mockResolvedValue({ data: ws });
    await expect(createWorkspace('Trimestral')).resolves.toEqual(ws);
    expect(mockedApi.post).toHaveBeenCalledWith('/workspace/workspaces', { nome: 'Trimestral' });
  });

  it('getWorkspace loads layout by id', async () => {
    mockedApi.get.mockResolvedValue({ data: { id: 7, nome: 'Trimestral', widgets: [] } });
    await getWorkspace(7);
    expect(mockedApi.get).toHaveBeenCalledWith('/workspace/workspaces/7');
  });
});
