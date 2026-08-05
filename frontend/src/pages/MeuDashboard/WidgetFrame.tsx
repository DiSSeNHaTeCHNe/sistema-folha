import { Box, Card, CardContent, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import type { ReactNode } from 'react';
import { widgetCardSx } from './widgets/cardStyles';

interface WidgetFrameProps {
  title: string;
  editMode?: boolean;
  toolbar?: ReactNode;
  badge?: ReactNode;
  children: ReactNode;
}

export function WidgetFrame({ title, editMode = false, toolbar, badge, children }: WidgetFrameProps) {
  const theme = useTheme();

  if (editMode) {
    return (
      <Card sx={widgetCardSx(theme)}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" gap={1} mb={toolbar ? 0 : undefined}>
            <Typography variant="subtitle1">{title}</Typography>
            {badge}
          </Box>
          {toolbar}
          {children}
        </CardContent>
      </Card>
    );
  }

  return (
    <Box aria-label={title} sx={{ height: '100%' }}>
      {badge ? (
        <Box display="flex" justifyContent="flex-end" mb={0.5}>
          {badge}
        </Box>
      ) : null}
      {children}
    </Box>
  );
}
