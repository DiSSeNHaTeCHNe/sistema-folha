import type { ReactNode } from 'react';
import { Box, Stack, Typography } from '@mui/material';
import { colors } from '../workspaceTheme';

export interface WorkspacePageShellProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
}

export function WorkspacePageShell({ title, subtitle, actions, children }: WorkspacePageShellProps) {
  return (
    <Box
      sx={{
        minHeight: '100%',
        bgcolor: colors.page,
        mx: -3,
        mt: -3,
        px: 3,
        py: 3,
      }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        spacing={2}
        sx={{ mb: 3 }}
      >
        <Box>
          <Typography variant="h5" component="h1" sx={{ color: colors.navy, fontWeight: 600 }}>
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body2" sx={{ color: colors.soft, mt: 0.5 }}>
              {subtitle}
            </Typography>
          ) : null}
        </Box>
        {actions ? <Box>{actions}</Box> : null}
      </Stack>
      {children}
    </Box>
  );
}
