import { Box, Card, CardContent, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { Cell, Pie, PieChart as RePieChart, Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { PieLegendEntry } from './chartUtils';
import { widgetCardSx } from './cardStyles';

interface DistribuicaoWidgetProps {
  title: string;
  data: PieLegendEntry[];
  currency?: boolean;
  tipoVisualizacao?: 'PIE' | 'BAR';
}

function formatLegendValue(value: number, currency: boolean) {
  if (currency || (typeof value === 'number' && value > 1000)) {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
  }
  return value;
}

function renderLegend(data: PieLegendEntry[], currency: boolean) {
  return (
    <Box mt={2}>
      {data.map((entry, index) => (
        <Box key={index} display="flex" alignItems="center" mb={1}>
          <Box width={12} height={12} bgcolor={entry.color} borderRadius="50%" mr={1} />
          <Typography variant="caption" color="text.secondary" title={entry.fullName || entry.name}>
            {entry.name}: {formatLegendValue(entry.value, currency)}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

export function DistribuicaoWidget({
  title,
  data,
  currency = false,
  tipoVisualizacao = 'PIE',
}: DistribuicaoWidgetProps) {
  const theme = useTheme();
  const isBar = tipoVisualizacao === 'BAR';

  return (
    <Card sx={widgetCardSx(theme)}>
      <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Typography variant="h6" gutterBottom>
          {title}
        </Typography>
        <Box flex={1} display="flex" flexDirection="column">
          {data.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={300}>
                {isBar ? (
                  <BarChart data={data} layout="vertical" margin={{ left: 80 }}>
                    <XAxis type="number" tickFormatter={(value) => String(formatLegendValue(Number(value), currency))} />
                    <YAxis type="category" dataKey="name" width={80} />
                    <Tooltip
                      formatter={(value) =>
                        currency
                          ? [Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }), 'Custo']
                          : [value, 'Quantidade']
                      }
                    />
                    <Bar dataKey="value" fill={theme.palette.charts[0]} />
                  </BarChart>
                ) : (
                  <RePieChart>
                    <Pie data={data} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={5} dataKey="value">
                      {data.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value) =>
                        currency
                          ? [Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }), 'Custo']
                          : [value, 'Quantidade']
                      }
                    />
                  </RePieChart>
                )}
              </ResponsiveContainer>
              {renderLegend(data, currency)}
            </>
          ) : (
            <Box flex={1} display="flex" alignItems="center" justifyContent="center">
              <Typography color="text.secondary">Nenhum dado disponível</Typography>
            </Box>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}
