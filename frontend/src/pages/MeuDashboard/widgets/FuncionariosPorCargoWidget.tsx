import { useTheme } from '@mui/material/styles';
import type { WidgetProps } from './registry';
import { resolveTopN } from '../widgetConfigOptions';
import { buildFuncionariosPorCargoPie } from './chartUtils';
import { DistribuicaoWidget } from './DistribuicaoWidget';

export function FuncionariosPorCargoWidget({ instance, data: widgetData }: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(instance.widgetId, instance.config);
  const chartData = buildFuncionariosPorCargoPie(widgetData, theme.palette.charts, topN);

  return (
    <DistribuicaoWidget
      title="Funcionários por Cargo"
      data={chartData}
      tipoVisualizacao={instance.config?.tipoVisualizacao}
    />
  );
}
