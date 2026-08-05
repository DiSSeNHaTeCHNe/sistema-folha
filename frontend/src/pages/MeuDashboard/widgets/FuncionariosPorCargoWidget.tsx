import { useTheme } from '@mui/material/styles';
import type { WidgetProps } from './registry';
import { resolveTopN } from '../widgetConfigOptions';
import { buildFuncionariosPorCargoPie } from './chartUtils';
import { DistribuicaoWidget } from './DistribuicaoWidget';

export function FuncionariosPorCargoWidget({ instance, stats }: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(instance.widgetId, instance.config);
  const data = stats ? buildFuncionariosPorCargoPie(stats, theme.palette.charts, topN) : [];

  return (
    <DistribuicaoWidget
      title="Funcionários por Cargo"
      data={data}
      tipoVisualizacao={instance.config?.tipoVisualizacao}
    />
  );
}
