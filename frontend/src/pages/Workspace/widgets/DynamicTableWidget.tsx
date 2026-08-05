import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { formatMoneyDisplay } from '../../../utils/money';
import type { WorkspaceWidgetData } from '../types';

interface DynamicTableWidgetProps {
  title: string;
  data: WorkspaceWidgetData;
}

function formatCell(value: string): string {
  if (/^\d+([.,]\d+)?$/.test(value.replace(/R\$\s?/g, '').trim())) {
    return formatMoneyDisplay(value);
  }
  return value;
}

export function DynamicTableWidget({ title, data }: DynamicTableWidgetProps) {
  if (data.linhas.length === 0) {
    return (
      <Typography color="text.secondary" role="status">
        Sem linhas para {title}
      </Typography>
    );
  }

  const columns = Object.keys(data.linhas[0] ?? {});

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small" aria-label={`Tabela ${title}`}>
        <TableHead>
          <TableRow>
            {columns.map((column) => (
              <TableCell key={column}>{column}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {data.linhas.map((row, index) => (
            <TableRow key={`row-${index}`}>
              {columns.map((column) => (
                <TableCell key={column}>{formatCell(row[column] ?? '')}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
