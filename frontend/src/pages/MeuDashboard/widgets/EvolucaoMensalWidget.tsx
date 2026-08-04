import { Box, Card, CardContent, Chip, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { WidgetProps } from './registry';
import { widgetCardSx } from './cardStyles';

export function EvolucaoMensalWidget({ stats }: WidgetProps) {
  const theme = useTheme();
  const chartColors = theme.palette.charts;
  const areaChartColor = chartColors[0];

  const areaData = (stats?.evolucaoMensal ?? []).map((item) => ({
    mes: item.mesAno,
    folha: item.valorTotal,
    funcionarios: item.quantidadeFuncionarios,
  }));

  return (
    <Card sx={widgetCardSx(theme)}>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">Evolução da Folha de Pagamento</Typography>
          <Chip label="Últimos 12 meses" variant="outlined" size="small" />
        </Box>
        {areaData.length > 0 ? (
          <ResponsiveContainer width="100%" height={350}>
            <AreaChart data={areaData}>
              <defs>
                <linearGradient id="meuDashboardColorFolha" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={areaChartColor} stopOpacity={0.8} />
                  <stop offset="95%" stopColor={areaChartColor} stopOpacity={0.1} />
                </linearGradient>
              </defs>
              <XAxis dataKey="mes" />
              <YAxis tickFormatter={(value) => `R$ ${Number(value).toLocaleString()}`} />
              <Tooltip
                formatter={(value, name) => [
                  name === 'folha' ? `R$ ${Number(value).toLocaleString()}` : value,
                  name === 'folha' ? 'Folha de Pagamento' : 'Funcionários',
                ]}
              />
              <Area
                type="monotone"
                dataKey="folha"
                stroke={areaChartColor}
                fillOpacity={1}
                fill="url(#meuDashboardColorFolha)"
                strokeWidth={3}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <Box display="flex" alignItems="center" justifyContent="center" height={350}>
            <Typography color="text.secondary" align="center">
              Nenhuma folha regular encontrada nos últimos 12 meses.
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}
