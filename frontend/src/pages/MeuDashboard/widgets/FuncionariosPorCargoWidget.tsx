import { useTheme } from '@mui/material/styles';
import type { WidgetProps } from './registry';
import { buildFuncionariosPorCargoPie } from './chartUtils';
import { DistribuicaoWidget } from './DistribuicaoWidget';

export function FuncionariosPorCargoWidget({ stats }: WidgetProps) {
  const theme = useTheme();
  const data = stats ? buildFuncionariosPorCargoPie(stats, theme.palette.charts) : [];

  return <DistribuicaoWidget title="Funcionários por Cargo" data={data} />;
}
