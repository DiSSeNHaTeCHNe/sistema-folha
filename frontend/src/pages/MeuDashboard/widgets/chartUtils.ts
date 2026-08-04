import type { DashboardStats } from '../../../services/dashboardService';

export type PieLegendEntry = {
  name: string;
  value: number;
  color: string;
  fullName?: string;
};

export function buildPieData(
  items: { descricao: string; quantidadeFuncionarios?: number; valorTotal?: number }[],
  valueKey: 'quantidadeFuncionarios' | 'valorTotal',
  chartColors: readonly string[],
  maxItems: number,
  nameMaxLength: number,
): PieLegendEntry[] {
  return items.slice(0, maxItems).map((item, index) => ({
    name:
      item.descricao.length > nameMaxLength
        ? `${item.descricao.substring(0, nameMaxLength)}...`
        : item.descricao,
    value: (item[valueKey] ?? 0) as number,
    color: chartColors[index % chartColors.length],
    fullName: item.descricao,
  }));
}

export function buildFuncionariosPorCentroPie(stats: DashboardStats, chartColors: readonly string[]) {
  return buildPieData(stats.porCentroCusto, 'quantidadeFuncionarios', chartColors, 5, 15);
}

export function buildFuncionariosPorLinhaPie(stats: DashboardStats, chartColors: readonly string[]) {
  return buildPieData(stats.porLinhaNegocio, 'quantidadeFuncionarios', chartColors, 6, 12);
}

export function buildCustoPorCentroPie(stats: DashboardStats, chartColors: readonly string[]) {
  return buildPieData(stats.porCentroCusto, 'valorTotal', chartColors, 6, 12);
}

export function buildCustoPorLinhaPie(stats: DashboardStats, chartColors: readonly string[]) {
  return buildPieData(stats.porLinhaNegocio, 'valorTotal', chartColors, 6, 12);
}

export function buildFuncionariosPorCargoPie(stats: DashboardStats, chartColors: readonly string[]) {
  return buildPieData(stats.porCargo ?? [], 'quantidadeFuncionarios', chartColors, 6, 12);
}
