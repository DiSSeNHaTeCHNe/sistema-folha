import { Box, Card, CardContent, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import type { ReactNode } from 'react';
import { widgetCardSx } from './widgets/cardStyles';

interface WidgetFrameProps {
  title: string;
  editMode?: boolean;
  toolbar?: ReactNode;
  children: ReactNode;
}

export function WidgetFrame({ title, editMode = false, toolbar, children }: WidgetFrameProps) {
  const theme = useTheme();

  if (editMode) {
    return (
      <Card sx={widgetCardSx(theme)}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            {title}
          </Typography>
          {toolbar}
          {children}
        </CardContent>
      </Card>
    );
  }

  return (
    <Box aria-label={title} sx={{ height: '100%' }}>
      {children}
    </Box>
  );
}
