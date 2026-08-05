import type { WidgetConfig } from './types';

export type ConfigField =
  | 'competencia'
  | 'topN'
  | 'dimensao'
  | 'metrica'
  | 'tipoVisualizacao'
  | 'centroCustoId'
  | 'linhaNegocioId';

const CC_LN_FIELDS: ConfigField[] = ['centroCustoId', 'linhaNegocioId'];

const WIDGET_CONFIG_FIELDS: Record<string, ConfigField[]> = {
  'kpi-total-funcionarios': ['competencia', ...CC_LN_FIELDS],
  'kpi-custo-empresa': ['competencia', ...CC_LN_FIELDS],
  'kpi-beneficios-ativos': ['competencia', ...CC_LN_FIELDS],
  'kpi-relacao-pd': ['competencia', ...CC_LN_FIELDS],
  'grafico-evolucao-mensal': ['competencia', ...CC_LN_FIELDS],
  'grafico-funcionarios-por-cc': ['competencia', 'topN', 'tipoVisualizacao', ...CC_LN_FIELDS],
  'grafico-funcionarios-por-linha': ['competencia', 'topN', 'tipoVisualizacao', ...CC_LN_FIELDS],
  'grafico-custo-por-cc': ['competencia', 'topN', 'tipoVisualizacao', ...CC_LN_FIELDS],
  'grafico-custo-por-linha': ['competencia', 'topN', 'tipoVisualizacao', ...CC_LN_FIELDS],
  'lista-top-proventos': ['competencia', 'topN', ...CC_LN_FIELDS],
  'lista-top-descontos': ['competencia', 'topN', ...CC_LN_FIELDS],
  'grafico-funcionarios-por-cargo': ['competencia', 'topN', 'tipoVisualizacao', ...CC_LN_FIELDS],
};

export function getConfigFieldsForWidget(widgetId: string): ConfigField[] {
  return WIDGET_CONFIG_FIELDS[widgetId] ?? [];
}

export function widgetSupportsConfig(widgetId: string): boolean {
  return getConfigFieldsForWidget(widgetId).length > 0;
}

export const TOP_N_MIN = 1;
export const TOP_N_MAX = 50;

export const DIMENSAO_OPTIONS = [
  { value: 'CENTRO_CUSTO', label: 'Centro de Custo' },
  { value: 'LINHA_NEGOCIO', label: 'Linha de Negócio' },
  { value: 'CARGO', label: 'Cargo' },
] as const;

export const METRICA_OPTIONS = [
  { value: 'FUNCIONARIOS', label: 'Funcionários' },
  { value: 'CUSTO', label: 'Custo' },
] as const;

export const TIPO_VISUALIZACAO_OPTIONS = [
  { value: 'PIE', label: 'Pizza' },
  { value: 'BAR', label: 'Barras' },
] as const;

export function defaultTopNForWidget(widgetId: string): number {
  switch (widgetId) {
    case 'grafico-funcionarios-por-cc':
      return 5;
    case 'grafico-funcionarios-por-linha':
    case 'grafico-custo-por-cc':
    case 'grafico-custo-por-linha':
    case 'grafico-funcionarios-por-cargo':
      return 6;
    case 'lista-top-proventos':
    case 'lista-top-descontos':
      return 5;
    default:
      return TOP_N_MAX;
  }
}

export function resolveTopN(widgetId: string, config?: WidgetConfig | null): number {
  return config?.topN ?? defaultTopNForWidget(widgetId);
}
