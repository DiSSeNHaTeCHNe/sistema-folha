import { Typography } from '@mui/material';
import { colors } from '../workspaceTheme';

export interface InlineCellErrorProps {
  message: string;
}

export function InlineCellError({ message }: InlineCellErrorProps) {
  return (
    <Typography
      variant="caption"
      role="alert"
      sx={{ color: colors.danger, display: 'block', mt: 0.5 }}
    >
      {message}
    </Typography>
  );
}
