import { Box, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { formatMoneyDisplay } from '../../../utils/money';
import type { WorkspaceWidgetData } from '../types';

interface DynamicChartWidgetProps {
  title: string;
  data: WorkspaceWidgetData;
  variant: 'GRAFICO_LINHA' | 'GRAFICO_BARRA';
}

function toChartRows(data: WorkspaceWidgetData) {
  return data.linhas.map((row, index) => {
    const label = row.label ?? row.competencia ?? row.nome ?? `Item ${index + 1}`;
    const rawValue = row.valor ?? row.total ?? Object.values(row).find((v) => /^\d/.test(v)) ?? '0';
    const numeric = Number.parseFloat(String(rawValue).replace(/R\$\s?/g, '').replace(',', '.'));
    return { label, value: Number.isNaN(numeric) ? 0 : numeric, display: formatMoneyDisplay(String(rawValue)) };
  });
}

export function DynamicChartWidget({ title, data, variant }: DynamicChartWidgetProps) {
  const theme = useTheme();
  const rows = toChartRows(data);

  if (rows.length === 0) {
    return (
      <Typography color="text.secondary" role="status">
        Sem dados para gráfico {title}
      </Typography>
    );
  }

  return (
    <Box aria-label={`Gráfico ${title}`} sx={{ width: '100%', height: 260 }}>
      <ResponsiveContainer width="100%" height="100%">
        {variant === 'GRAFICO_BARRA' ? (
          <BarChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="label" />
            <YAxis />
            <Tooltip formatter={(value: number) => formatMoneyDisplay(value)} />
            <Legend />
            <Bar dataKey="value" fill={theme.palette.primary.main} name={title} />
          </BarChart>
        ) : (
          <LineChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="label" />
            <YAxis />
            <Tooltip formatter={(value: number) => formatMoneyDisplay(value)} />
            <Legend />
            <Line type="monotone" dataKey="value" stroke={theme.palette.primary.main} name={title} />
          </LineChart>
        )}
      </ResponsiveContainer>
    </Box>
  );
}
