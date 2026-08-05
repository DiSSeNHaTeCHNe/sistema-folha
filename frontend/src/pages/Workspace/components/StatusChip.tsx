import { Chip } from '@mui/material';
import { chipVariants, type ChipVariant } from '../workspaceTheme';

export interface StatusChipProps {
  variant: ChipVariant;
  label: string;
}

export function StatusChip({ variant, label }: StatusChipProps) {
  const styles = chipVariants[variant];

  return (
    <Chip
      label={label}
      size="small"
      sx={{
        color: styles.color,
        bgcolor: styles.backgroundColor,
        border: `1px solid ${styles.borderColor}`,
        fontWeight: 600,
      }}
    />
  );
}
