import { Avatar, Box, Card, CardContent, Divider, List, ListItem, ListItemAvatar, ListItemText, Typography } from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import { TrendingDown, TrendingUp } from '@mui/icons-material';
import type { RubricaStats } from '../../../services/dashboardService';
import type { WidgetProps } from './registry';
import { widgetCardSx } from './cardStyles';

interface TopRubricasWidgetProps extends WidgetProps {
  variant: 'proventos' | 'descontos';
}

export function TopRubricasWidget({ stats, variant }: TopRubricasWidgetProps) {
  const theme = useTheme();
  const chartColors = theme.palette.charts;
  const items: RubricaStats[] =
    variant === 'proventos' ? stats?.topProventos ?? [] : stats?.topDescontos ?? [];
  const title = variant === 'proventos' ? 'Top 5 Proventos' : 'Top 5 Descontos';
  const avatarColor = variant === 'proventos' ? 'success' : 'error';
  const valueColor = variant === 'proventos' ? 'success.main' : 'error.main';
  const Icon = variant === 'proventos' ? TrendingUp : TrendingDown;

  return (
    <Card sx={widgetCardSx(theme)}>
      <CardContent>
        <Box display="flex" alignItems="center" mb={2}>
          <Avatar sx={{ backgroundColor: `${avatarColor}.light`, color: `${avatarColor}.main`, mr: 2 }}>
            <Icon />
          </Avatar>
          <Typography variant="h6">{title}</Typography>
        </Box>
        <Divider sx={{ mb: 2 }} />
        <List dense>
          {items.map((item, index) => (
            <ListItem key={item.id} sx={{ borderRadius: 2, mb: 1, '&:hover': { bgcolor: 'action.hover' } }}>
              <ListItemAvatar>
                <Avatar
                  sx={{
                    background: `linear-gradient(135deg, ${chartColors[index % chartColors.length]} 0%, ${alpha(chartColors[index % chartColors.length], 0.5)} 100%)`,
                    color: 'common.white',
                    fontWeight: 'bold',
                    fontSize: '0.8rem',
                  }}
                >
                  #{index + 1}
                </Avatar>
              </ListItemAvatar>
              <ListItemText
                primary={<Typography variant="subtitle2">{item.codigo} - {item.descricao}</Typography>}
                secondary={
                  <Box>
                    <Typography variant="body2" color={valueColor}>
                      {item.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.quantidadeOcorrencias} ocorrências
                    </Typography>
                  </Box>
                }
              />
            </ListItem>
          ))}
        </List>
      </CardContent>
    </Card>
  );
}
