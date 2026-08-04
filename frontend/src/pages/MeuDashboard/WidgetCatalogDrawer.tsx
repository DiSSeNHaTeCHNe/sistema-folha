import CloseIcon from '@mui/icons-material/Close';
import {
  Alert,
  Box,
  Button,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
} from '@mui/material';
import type { WidgetCatalogItem, WidgetInstance } from './types';
import { MAX_WIDGETS } from './types';

interface WidgetCatalogDrawerProps {
  open: boolean;
  onClose: () => void;
  catalog: WidgetCatalogItem[];
  widgets: WidgetInstance[];
  onAddWidget: (item: WidgetCatalogItem) => void;
  onLimitReached?: (message: string) => void;
}

export function WidgetCatalogDrawer({
  open,
  onClose,
  catalog,
  widgets,
  onAddWidget,
  onLimitReached,
}: WidgetCatalogDrawerProps) {
  const addedIds = new Set(widgets.map((widget) => widget.widgetId));
  const atLimit = widgets.length >= MAX_WIDGETS;

  const handleAdd = (item: WidgetCatalogItem) => {
    if (atLimit) {
      onLimitReached?.('Limite de 30 widgets atingido');
      return;
    }
    if (addedIds.has(item.widgetId)) {
      return;
    }
    onAddWidget(item);
  };

  return (
    <Drawer anchor="right" open={open} onClose={onClose} aria-label="Catálogo de widgets">
      <Box sx={{ width: 360, p: 2 }} role="presentation">
        <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
          <Typography variant="h6">Adicionar widget</Typography>
          <IconButton aria-label="Fechar catálogo" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>
        {atLimit && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Limite de 30 widgets atingido
          </Alert>
        )}
        <List>
          {catalog.map((item) => {
            const alreadyAdded = addedIds.has(item.widgetId);
            return (
              <ListItem key={item.widgetId} disablePadding>
                <ListItemButton
                  disabled={alreadyAdded || atLimit}
                  onClick={() => handleAdd(item)}
                  aria-label={`Adicionar ${item.titulo}`}
                >
                  <ListItemText
                    primary={item.titulo}
                    secondary={alreadyAdded ? 'Já adicionado' : item.descricao}
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      </Box>
    </Drawer>
  );
}

interface DashboardEmptyStateProps {
  editMode: boolean;
  onAddWidgets: () => void;
}

export function DashboardEmptyState({ editMode, onAddWidgets }: DashboardEmptyStateProps) {
  return (
    <Box
      sx={{
        border: 1,
        borderColor: 'divider',
        borderRadius: 3,
        p: 4,
        textAlign: 'center',
      }}
    >
      <Typography variant="h6" gutterBottom>
        Seu dashboard está vazio
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {editMode
          ? 'Adicione widgets do catálogo para montar sua visão personalizada.'
          : 'Entre no modo de edição para adicionar widgets.'}
      </Typography>
      {editMode && (
        <Button variant="contained" onClick={onAddWidgets}>
          Adicionar widgets
        </Button>
      )}
    </Box>
  );
}
