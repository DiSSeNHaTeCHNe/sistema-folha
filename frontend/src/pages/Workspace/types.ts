export type DatasetFieldType = 'NUMERO' | 'TEXTO' | 'DATA' | 'MOEDA' | 'REFERENCIA';

export type ReferenciaEntidade = 'CENTRO_CUSTO' | 'FUNCIONARIO' | 'CARGO';

export interface DatasetFieldSchema {
  nome: string;
  tipo: DatasetFieldType;
  referenciaEntidade?: ReferenciaEntidade | null;
  obrigatorio?: boolean | null;
}

export interface DatasetSummary {
  id: number;
  nome: string;
  schemaVersion: number;
  totalLinhas: number;
  totalCampos: number;
}

export interface DatasetDefinition {
  id: number;
  nome: string;
  campos: DatasetFieldSchema[];
  schemaVersion: number;
  totalLinhas: number;
}

export interface DatasetRow {
  id: number;
  datasetId: number;
  valores: Record<string, unknown>;
  ordem: number | null;
}

export type WidgetSourceKind = 'DATASET' | 'SISTEMA';

export interface WidgetSourceRef {
  kind: WidgetSourceKind;
  ref: string;
}

export type UserWidgetTipo = 'KPI' | 'TABELA' | 'GRAFICO_LINHA' | 'GRAFICO_BARRA';

export interface UserWidgetDefinition {
  id: number;
  nome: string;
  tipo: UserWidgetTipo;
  fontes: WidgetSourceRef[];
  formula: string | null;
  config: Record<string, unknown>;
  invalido: boolean;
}

export interface WorkspaceLayoutWidget {
  instanceId: string;
  ordem: number;
  colSpan: number;
  rowSpan: number;
  widgetId?: string | null;
  userWidgetDefinitionId?: number | null;
  config?: Record<string, unknown> | null;
}

export interface WorkspaceSummary {
  id: number;
  nome: string;
  totalWidgets: number;
}

export interface Workspace {
  id: number;
  nome: string;
  widgets: WorkspaceLayoutWidget[];
}

export interface WorkspaceWidgetData {
  instanceId: string;
  userWidgetDefinitionId: number | null;
  widgetId: string | null;
  tipo: string;
  semDados: boolean;
  invalido: boolean;
  competencia: string | null;
  valores: Record<string, string>;
  linhas: Record<string, string>[];
}

export interface OrcamentoInstallResult {
  workspaceId: number;
  datasetId: number;
  widgetDefinitionIds: number[];
  widgetsAdicionados: number;
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export const MAX_WORKSPACE_WIDGETS = 30;

export const COL_SPAN_PRESETS = {
  P: 3,
  M: 4,
  G: 6,
  Full: 12,
} as const;

export type ColSpanPreset = keyof typeof COL_SPAN_PRESETS;
