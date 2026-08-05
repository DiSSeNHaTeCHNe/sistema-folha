import type { ReactNode } from 'react';
import { Box } from '@mui/material';
import { InfoBanner } from './InfoBanner';

export interface WidgetErrorBannerProps {
  message: string;
  title?: string;
  variant?: 'warn' | 'danger';
  action?: ReactNode;
}

export function WidgetErrorBanner({
  message,
  title,
  variant = 'danger',
  action,
}: WidgetErrorBannerProps) {
  return (
    <Box sx={{ mb: 1 }}>
      <InfoBanner variant={variant === 'warn' ? 'warn' : 'danger'} title={title}>
        {message}
        {action ? <Box sx={{ mt: 1 }}>{action}</Box> : null}
      </InfoBanner>
    </Box>
  );
}
