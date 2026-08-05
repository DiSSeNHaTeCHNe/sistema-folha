import type { WidgetData } from '../types';

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

export function buildFuncionariosPorCentroPie(
  data: Pick<WidgetData, 'porCentroCusto'>,
  chartColors: readonly string[],
  maxItems = 5,
) {
  return buildPieData(data.porCentroCusto ?? [], 'quantidadeFuncionarios', chartColors, maxItems, 15);
}

export function buildFuncionariosPorLinhaPie(
  data: Pick<WidgetData, 'porLinhaNegocio'>,
  chartColors: readonly string[],
  maxItems = 6,
) {
  return buildPieData(data.porLinhaNegocio ?? [], 'quantidadeFuncionarios', chartColors, maxItems, 12);
}

export function buildCustoPorCentroPie(
  data: Pick<WidgetData, 'porCentroCusto'>,
  chartColors: readonly string[],
  maxItems = 6,
) {
  return buildPieData(data.porCentroCusto ?? [], 'valorTotal', chartColors, maxItems, 12);
}

export function buildCustoPorLinhaPie(
  data: Pick<WidgetData, 'porLinhaNegocio'>,
  chartColors: readonly string[],
  maxItems = 6,
) {
  return buildPieData(data.porLinhaNegocio ?? [], 'valorTotal', chartColors, maxItems, 12);
}

export function buildFuncionariosPorCargoPie(
  data: Pick<WidgetData, 'porCargo'>,
  chartColors: readonly string[],
  maxItems = 6,
) {
  return buildPieData(data.porCargo ?? [], 'quantidadeFuncionarios', chartColors, maxItems, 12);
}
