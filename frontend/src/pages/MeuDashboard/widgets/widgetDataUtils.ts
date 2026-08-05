import type {
  CargoStats,
  CentroCustoStats,
  LinhaNegocioStats,
} from '../../services/dashboardService';
import type { PieLegendEntry } from './widgets/chartUtils';

export function buildPieFromStatsItems(
  items: (CentroCustoStats | LinhaNegocioStats | CargoStats)[] | null | undefined,
  valueKey: 'quantidadeFuncionarios' | 'valorTotal',
  chartColors: readonly string[],
  nameMaxLength: number,
): PieLegendEntry[] {
  if (!items?.length) {
    return [];
  }
  return items.map((item, index) => ({
    name:
      item.descricao.length > nameMaxLength
        ? `${item.descricao.substring(0, nameMaxLength)}...`
        : item.descricao,
    value: (item[valueKey] ?? 0) as number,
    color: chartColors[index % chartColors.length],
    fullName: item.descricao,
  }));
}
