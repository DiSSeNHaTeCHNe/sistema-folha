import type { ComponentType } from 'react';
import {
  getWidgetDefinition as getDashboardWidgetDefinition,
  WIDGET_REGISTRY,
  type WidgetDefinition as DashboardWidgetDefinition,
} from '../../MeuDashboard/widgets/registry';
import type { UserWidgetTipo, WorkspaceLayoutWidget, WorkspaceWidgetData } from '../types';
import { DynamicChartWidget } from './DynamicChartWidget';
import { DynamicKpiWidget } from './DynamicKpiWidget';
import { DynamicTableWidget } from './DynamicTableWidget';

export const USER_WIDGET_PREFIX = 'USER_';

export type WorkspaceWidgetKind = UserWidgetTipo | 'CATALOG';

export interface WorkspaceWidgetDefinition {
  id: string;
  titulo: string;
  kind: WorkspaceWidgetKind;
  colSpanPadrao: number;
  rowSpanPadrao: number;
  userWidgetDefinitionId?: number;
  catalogWidgetId?: string;
  Component: ComponentType<{ title: string; data: WorkspaceWidgetData }>;
}

function userRegistryId(tipo: UserWidgetTipo): string {
  return `${USER_WIDGET_PREFIX}${tipo}`;
}

function buildUserComponent(tipo: UserWidgetTipo): ComponentType<{ title: string; data: WorkspaceWidgetData }> {
  switch (tipo) {
    case 'KPI':
      return DynamicKpiWidget;
    case 'TABELA':
      return DynamicTableWidget;
    case 'GRAFICO_LINHA':
      return ({ title, data }) => <DynamicChartWidget title={title} data={data} variant="GRAFICO_LINHA" />;
    case 'GRAFICO_BARRA':
      return ({ title, data }) => <DynamicChartWidget title={title} data={data} variant="GRAFICO_BARRA" />;
    default:
      return DynamicKpiWidget;
  }
}

const USER_WIDGET_DEFINITIONS: WorkspaceWidgetDefinition[] = (
  ['KPI', 'TABELA', 'GRAFICO_LINHA', 'GRAFICO_BARRA'] as UserWidgetTipo[]
).map((tipo) => ({
  id: userRegistryId(tipo),
  titulo: `Widget ${tipo}`,
  kind: tipo,
  colSpanPadrao: tipo === 'KPI' ? 3 : 6,
  rowSpanPadrao: 1,
  Component: buildUserComponent(tipo),
}));

const CATALOG_WIDGET_DEFINITIONS: WorkspaceWidgetDefinition[] = WIDGET_REGISTRY.map(
  (item: DashboardWidgetDefinition) => ({
    id: item.id,
    titulo: item.titulo,
    kind: 'CATALOG' as const,
    colSpanPadrao: item.colSpanPadrao,
    rowSpanPadrao: item.rowSpanPadrao,
    catalogWidgetId: item.id,
    Component: DynamicKpiWidget,
  }),
);

export const WORKSPACE_WIDGET_REGISTRY: WorkspaceWidgetDefinition[] = [
  ...CATALOG_WIDGET_DEFINITIONS,
  ...USER_WIDGET_DEFINITIONS,
];

export function getWorkspaceWidgetDefinition(
  widget: WorkspaceLayoutWidget,
  userDefinitions: Map<number, { nome: string; tipo: UserWidgetTipo; invalido: boolean }>,
): WorkspaceWidgetDefinition | null {
  if (widget.userWidgetDefinitionId != null) {
    const userDef = userDefinitions.get(widget.userWidgetDefinitionId);
    if (!userDef) {
      return null;
    }
    const base = USER_WIDGET_DEFINITIONS.find((item) => item.kind === userDef.tipo);
    if (!base) {
      return null;
    }
    return {
      ...base,
      titulo: userDef.nome,
      userWidgetDefinitionId: widget.userWidgetDefinitionId,
    };
  }

  if (widget.widgetId) {
    const catalog = getDashboardWidgetDefinition(widget.widgetId);
    if (!catalog) {
      return null;
    }
    return CATALOG_WIDGET_DEFINITIONS.find((item) => item.catalogWidgetId === widget.widgetId) ?? null;
  }

  return null;
}

export function resolveUserWidgetRegistryId(tipo: UserWidgetTipo): string {
  return userRegistryId(tipo);
}

export { getDashboardWidgetDefinition, WIDGET_REGISTRY };
