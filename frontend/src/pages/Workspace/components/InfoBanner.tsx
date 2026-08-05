import type { ReactNode } from 'react';
import { Box, Typography } from '@mui/material';
import { bannerVariants, type BannerVariant } from '../workspaceTheme';

export interface InfoBannerProps {
  variant: BannerVariant;
  children: ReactNode;
  title?: string;
}

export function InfoBanner({ variant, children, title }: InfoBannerProps) {
  const styles = bannerVariants[variant];

  return (
    <Box
      role={styles.role}
      sx={{
        color: styles.color,
        bgcolor: styles.backgroundColor,
        border: `1px solid ${styles.borderColor}`,
        borderRadius: 1,
        px: 2,
        py: 1.5,
      }}
    >
      {title ? (
        <Typography variant="subtitle2" component="p" sx={{ fontWeight: 600, mb: 0.5 }}>
          {title}
        </Typography>
      ) : null}
      <Typography variant="body2" component="div">
        {children}
      </Typography>
    </Box>
  );
}
