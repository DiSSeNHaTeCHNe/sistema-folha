import api from './api';
import type { WidgetConfig } from '../pages/MeuDashboard/types';
import type { WidgetData } from '../pages/MeuDashboard/types';

export interface WidgetDataQueryParams {
  competencia?: string;
  topN?: number;
  dimensao?: string;
  metrica?: string;
  tipoVisualizacao?: string;
  centroCustoId?: number;
  linhaNegocioId?: number;
  quantidadeMeses?: number;
}

export function buildWidgetQueryParams(
  config: WidgetConfig | null | undefined,
  competenciaGlobal: string | null,
): WidgetDataQueryParams {
  const params: WidgetDataQueryParams = {};
  const competencia = config?.competencia ?? competenciaGlobal ?? undefined;
  if (competencia) {
    params.competencia = competencia;
  }
  if (config?.topN != null) {
    params.topN = config.topN;
  }
  if (config?.dimensao) {
    params.dimensao = config.dimensao;
  }
  if (config?.metrica) {
    params.metrica = config.metrica;
  }
  if (config?.tipoVisualizacao) {
    params.tipoVisualizacao = config.tipoVisualizacao;
  }
  if (config?.centroCustoId != null) {
    params.centroCustoId = config.centroCustoId;
  }
  if (config?.linhaNegocioId != null) {
    params.linhaNegocioId = config.linhaNegocioId;
  }
  return params;
}

export async function getWidgetData(
  widgetId: string,
  params: WidgetDataQueryParams,
): Promise<WidgetData> {
  const response = await api.get<WidgetData>(`/dashboard/widgets/${widgetId}/data`, { params });
  return response.data;
}
