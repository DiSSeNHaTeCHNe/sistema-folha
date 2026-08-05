import type {
  CargoStats,
  CentroCustoStats,
  EvolucaoMensal,
  LinhaNegocioStats,
  RubricaStats,
} from '../../services/dashboardService';

export type WidgetCategoria = 'KPI' | 'GRAFICO' | 'LISTA';

export interface WidgetData {
  widgetId: string;
  competencia: string;
  semDados: boolean;
  totalFuncionarios?: number | null;
  custoMensalFolha?: string | number | null;
  totalBeneficiosAtivos?: number | null;
  totalProventos?: number | null;
  totalDescontos?: number | null;
  porLinhaNegocio?: LinhaNegocioStats[] | null;
  porCentroCusto?: CentroCustoStats[] | null;
  porCargo?: CargoStats[] | null;
  topProventos?: RubricaStats[] | null;
  topDescontos?: RubricaStats[] | null;
  evolucaoMensal?: EvolucaoMensal[] | null;
}

export interface WidgetConfig {
  competencia?: string;
  topN?: number;
  dimensao?: 'CENTRO_CUSTO' | 'LINHA_NEGOCIO' | 'CARGO';
  metrica?: 'FUNCIONARIOS' | 'CUSTO';
  tipoVisualizacao?: 'PIE' | 'BAR';
  centroCustoId?: number;
  linhaNegocioId?: number;
}

export interface WidgetInstance {
  widgetId: string;
  instanceId: string;
  ordem: number;
  colSpan: number;
  rowSpan: number;
  config?: WidgetConfig | null;
}

export interface DashboardLayout {
  id: number | null;
  nome: string;
  widgets: WidgetInstance[];
}

export interface WidgetCatalogItem {
  widgetId: string;
  titulo: string;
  descricao: string;
  categoria: WidgetCategoria;
  colSpanPadrao: number;
  rowSpanPadrao: number;
}

export const MAX_WIDGETS = 30;

export const COL_SPAN_PRESETS = {
  P: 3,
  M: 4,
  G: 6,
  Full: 12,
} as const;

export type ColSpanPreset = keyof typeof COL_SPAN_PRESETS;
