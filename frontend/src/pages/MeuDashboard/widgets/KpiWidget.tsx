import { Avatar, Box, Card, CardContent, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import type { ReactNode } from 'react';
import { widgetCardSx } from './cardStyles';

export interface KpiWidgetDisplayProps {
  title: string;
  value: ReactNode;
  icon: ReactNode;
  color: 'info' | 'success' | 'warning';
  avatarIconTone?: 'main' | 'dark';
  valueVariant?: 'h3' | 'h4';
  valueColor?: string;
}

export function KpiWidget({
  title,
  value,
  icon,
  color,
  avatarIconTone = 'main',
  valueVariant = 'h3',
  valueColor,
}: KpiWidgetDisplayProps) {
  const theme = useTheme();

  return (
    <Card sx={widgetCardSx(theme)}>
      <CardContent
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
        }}
      >
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
              {title}
            </Typography>
            <Typography variant={valueVariant} color={valueColor}>
              {value}
            </Typography>
          </Box>
          <Avatar
            sx={{
              backgroundColor: `${color}.light`,
              color: `${color}.${avatarIconTone}`,
              width: 56,
              height: 56,
            }}
          >
            {icon}
          </Avatar>
        </Box>
      </CardContent>
    </Card>
  );
}
