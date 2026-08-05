import axios from 'axios';
import api from './api';
import type {
  DatasetDefinition,
  DatasetFieldSchema,
  DatasetRow,
  DatasetSummary,
  FieldErrorItem,
  OrcamentoInstallResult,
  UserWidgetDefinition,
  Workspace,
  WorkspaceLayoutWidget,
  WorkspaceSummary,
  WorkspaceWidgetData,
} from '../pages/Workspace/types';

export class WorkspaceApiError extends Error {
  readonly status: number;

  readonly errors?: FieldErrorItem[];

  constructor(status: number, message: string, errors?: FieldErrorItem[]) {
    super(message);
    this.name = 'WorkspaceApiError';
    this.status = status;
    this.errors = errors;
  }
}

interface ApiErrorBody {
  status?: number;
  message?: string;
  errors?: FieldErrorItem[];
}

function rethrowAsWorkspaceApiError(error: unknown): never {
  if (axios.isAxiosError(error) && error.response) {
    const body = error.response.data as ApiErrorBody | undefined;
    throw new WorkspaceApiError(
      body?.status ?? error.response.status,
      body?.message ?? 'Erro na requisição ao workspace',
      body?.errors,
    );
  }
  throw error;
}

async function wrapRequest<T>(request: Promise<{ data: T }>): Promise<T> {
  try {
    const response = await request;
    return response.data;
  } catch (error) {
    rethrowAsWorkspaceApiError(error);
  }
}

// --- Datasets ---

export async function listDatasets(): Promise<DatasetSummary[]> {
  return wrapRequest(api.get<DatasetSummary[]>('/workspace/datasets'));
}

export async function createDataset(nome: string, campos: DatasetFieldSchema[]): Promise<DatasetDefinition> {
  return wrapRequest(api.post<DatasetDefinition>('/workspace/datasets', { nome, campos }));
}

export async function getDataset(id: number): Promise<DatasetDefinition> {
  return wrapRequest(api.get<DatasetDefinition>(`/workspace/datasets/${id}`));
}

export async function updateDatasetSchema(
  id: number,
  campos: DatasetFieldSchema[],
  schemaVersion: number,
  confirmarRemocao?: boolean,
): Promise<DatasetDefinition> {
  return wrapRequest(
    api.put<DatasetDefinition>(`/workspace/datasets/${id}/schema`, {
      campos,
      schemaVersion,
      confirmarRemocao: confirmarRemocao ?? false,
    }),
  );
}

export async function deleteDataset(id: number): Promise<void> {
  await wrapRequest(api.delete(`/workspace/datasets/${id}`));
}

export async function listDatasetRows(datasetId: number): Promise<DatasetRow[]> {
  return wrapRequest(api.get<DatasetRow[]>(`/workspace/datasets/${datasetId}/rows`));
}

export async function createDatasetRow(
  datasetId: number,
  valores: Record<string, unknown>,
): Promise<DatasetRow> {
  return wrapRequest(api.post<DatasetRow>(`/workspace/datasets/${datasetId}/rows`, { valores }));
}

export async function updateDatasetRow(
  datasetId: number,
  rowId: number,
  valores: Record<string, unknown>,
): Promise<DatasetRow> {
  return wrapRequest(api.put<DatasetRow>(`/workspace/datasets/${datasetId}/rows/${rowId}`, { valores }));
}

export async function deleteDatasetRow(datasetId: number, rowId: number): Promise<void> {
  await wrapRequest(api.delete(`/workspace/datasets/${datasetId}/rows/${rowId}`));
}

// --- Widget definitions ---

export async function listWidgetDefinitions(): Promise<UserWidgetDefinition[]> {
  return wrapRequest(api.get<UserWidgetDefinition[]>('/workspace/widget-definitions'));
}

export async function createWidgetDefinition(
  payload: Omit<UserWidgetDefinition, 'id' | 'invalido'>,
): Promise<UserWidgetDefinition> {
  return wrapRequest(api.post<UserWidgetDefinition>('/workspace/widget-definitions', payload));
}

export async function getWidgetDefinition(id: number): Promise<UserWidgetDefinition> {
  return wrapRequest(api.get<UserWidgetDefinition>(`/workspace/widget-definitions/${id}`));
}

export async function updateWidgetDefinition(
  id: number,
  payload: Omit<UserWidgetDefinition, 'id' | 'invalido'>,
): Promise<UserWidgetDefinition> {
  return wrapRequest(api.put<UserWidgetDefinition>(`/workspace/widget-definitions/${id}`, payload));
}

export async function deleteWidgetDefinition(id: number): Promise<void> {
  await wrapRequest(api.delete(`/workspace/widget-definitions/${id}`));
}

// --- Workspaces ---

export async function listWorkspaces(): Promise<WorkspaceSummary[]> {
  return wrapRequest(api.get<WorkspaceSummary[]>('/workspace/workspaces'));
}

export async function createWorkspace(nome: string): Promise<Workspace> {
  return wrapRequest(api.post<Workspace>('/workspace/workspaces', { nome }));
}

export async function getWorkspace(id: number): Promise<Workspace> {
  return wrapRequest(api.get<Workspace>(`/workspace/workspaces/${id}`));
}

export async function saveWorkspaceLayout(id: number, widgets: WorkspaceLayoutWidget[]): Promise<Workspace> {
  return wrapRequest(api.put<Workspace>(`/workspace/workspaces/${id}/layout`, { widgets }));
}

export async function deleteWorkspace(id: number): Promise<void> {
  await wrapRequest(api.delete(`/workspace/workspaces/${id}`));
}

// --- Widget data ---

export async function getWorkspaceWidgetData(
  workspaceId: number,
  instanceId: string,
  competencia?: string | null,
): Promise<WorkspaceWidgetData> {
  const params = competencia ? { competencia } : undefined;
  return wrapRequest(
    api.get<WorkspaceWidgetData>(
      `/workspace/workspaces/${workspaceId}/widgets/${instanceId}/data`,
      { params },
    ),
  );
}

// --- Templates (P1 native orçamento) ---

export async function installOrcamentoTemplate(workspaceId: number): Promise<OrcamentoInstallResult> {
  return wrapRequest(
    api.post<OrcamentoInstallResult>('/workspace/templates/orcamento-padrao/install', { workspaceId }),
  );
}
