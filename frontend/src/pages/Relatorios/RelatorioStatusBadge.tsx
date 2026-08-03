import { Chip } from '@mui/material';

export type RelatorioStatus = 'PENDENTE' | 'PROCESSADO' | 'ERRO';

const STATUS_CONFIG: Record<
  RelatorioStatus,
  { label: string; color: 'warning' | 'success' | 'error' }
> = {
  PENDENTE: { label: 'Pendente', color: 'warning' },
  PROCESSADO: { label: 'Processado', color: 'success' },
  ERRO: { label: 'Erro', color: 'error' },
};

interface RelatorioStatusBadgeProps {
  status: RelatorioStatus;
}

export function RelatorioStatusBadge({ status }: RelatorioStatusBadgeProps) {
  const config = STATUS_CONFIG[status];

  return (
    <Chip
      label={config.label}
      color={config.color}
      size="small"
      aria-label={`Status do relatório: ${config.label}`}
    />
  );
}
