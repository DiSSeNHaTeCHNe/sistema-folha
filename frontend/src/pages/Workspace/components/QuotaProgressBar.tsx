import { Box, LinearProgress, Tooltip, Typography } from '@mui/material';
import { colors } from '../workspaceTheme';
import { StatusChip } from './StatusChip';

export interface QuotaProgressBarProps {
  label: string;
  current: number;
  max: number;
}

function quotaVariant(current: number, max: number): 'ok' | 'warn' | 'danger' {
  if (current >= max) {
    return 'danger';
  }
  if (max > 0 && current / max >= 0.8) {
    return 'warn';
  }
  return 'ok';
}

export function QuotaProgressBar({ label, current, max }: QuotaProgressBarProps) {
  const percent = max > 0 ? Math.min(100, (current / max) * 100) : 0;
  const atLimit = current >= max;
  const variant = quotaVariant(current, max);
  const countLabel = `${current} de ${max}`;

  const bar = (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
        <Typography variant="body2" sx={{ color: colors.navy, fontWeight: 500 }}>
          {label}
        </Typography>
        {variant === 'ok' ? (
          <Typography variant="body2" sx={{ color: colors.soft }}>
            {countLabel}
          </Typography>
        ) : (
          <StatusChip variant={variant} label={countLabel} />
        )}
      </Box>
      <LinearProgress
        variant="determinate"
        value={percent}
        aria-valuenow={current}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={`${label}: ${countLabel}`}
        sx={{
          height: 8,
          borderRadius: 1,
          bgcolor: colors.line,
          '& .MuiLinearProgress-bar': {
            bgcolor: atLimit ? colors.danger : variant === 'warn' ? colors.warn : colors.violet,
            borderRadius: 1,
          },
          opacity: atLimit ? 0.85 : 1,
        }}
      />
    </Box>
  );

  if (atLimit) {
    return (
      <Tooltip title="Limite atingido. Remova itens ou entre em contato com o administrador." arrow>
        <Box component="span" sx={{ display: 'block', width: '100%' }}>
          {bar}
        </Box>
      </Tooltip>
    );
  }

  return bar;
}
