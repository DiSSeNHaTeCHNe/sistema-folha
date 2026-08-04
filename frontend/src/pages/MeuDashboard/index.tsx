import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { DashboardGrid } from './DashboardGrid';
import { DashboardEmptyState, WidgetCatalogDrawer } from './WidgetCatalogDrawer';
import { useDashboardLayout } from './hooks/useDashboardLayout';
import type { WidgetCatalogItem } from './types';
import { criarWidgetFromCatalog } from './widgetUtils';

export default function MeuDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [resetDialogOpen, setResetDialogOpen] = useState(false);
  const { notification, showNotification, hideNotification } = useNotification();
  const {
    activeLayout,
    catalog,
    editMode,
    loading: layoutLoading,
    saving,
    dirty,
    enterEditMode,
    cancelEdit,
    save,
    resetToDefault,
    updateDraftWidgets,
    draftLayout,
  } = useDashboardLayout();

  useEffect(() => {
    let cancelled = false;
    async function loadStats() {
      try {
        setStatsLoading(true);
        const data = await getDashboardStats();
        if (!cancelled) {
          setStats(data);
        }
      } catch {
        if (!cancelled) {
          showNotification('Erro ao carregar dados do dashboard', 'error');
        }
      } finally {
        if (!cancelled) {
          setStatsLoading(false);
        }
      }
    }
    void loadStats();
    return () => {
      cancelled = true;
    };
  }, [showNotification]);

  useEffect(() => {
    if (!dirty) {
      return;
    }
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  const widgets = useMemo(() => {
    if (!activeLayout) {
      return [];
    }
    return [...activeLayout.widgets].sort((a, b) => a.ordem - b.ordem);
  }, [activeLayout]);

  const handleAddWidget = (item: WidgetCatalogItem) => {
    if (!editMode || !draftLayout) {
      return;
    }
    const nextWidget = criarWidgetFromCatalog(item, draftLayout.widgets.length);
    updateDraftWidgets([...draftLayout.widgets, nextWidget]);
    setCatalogOpen(false);
  };

  const handleRemoveWidget = (instanceId: string) => {
    if (!editMode || !draftLayout) {
      return;
    }
    const filtered = draftLayout.widgets
      .filter((widget) => widget.instanceId !== instanceId)
      .map((widget, index) => ({ ...widget, ordem: index }));
    updateDraftWidgets(filtered);
  };

  const handleSave = async () => {
    const ok = await save();
    if (ok) {
      showNotification('Layout salvo com sucesso', 'success');
    } else {
      showNotification('Erro ao salvar layout', 'error');
    }
  };

  const handleReset = async () => {
    setResetDialogOpen(false);
    const ok = await resetToDefault();
    if (ok) {
      showNotification('Layout padrão restaurado', 'success');
    } else {
      showNotification('Erro ao restaurar layout padrão', 'error');
    }
  };

  const loading = layoutLoading || statsLoading;

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} aria-label="Carregando Meu Dashboard" />
      </Box>
    );
  }

  if (!stats || !activeLayout) {
    return <Alert severity="error">Erro ao carregar Meu Dashboard</Alert>;
  }

  return (
    <>
      <Box sx={{ backgroundColor: 'background.default', minHeight: '100vh' }}>
        <Box mb={4} display="flex" justifyContent="space-between" alignItems="flex-start" gap={2}>
          <Box>
            <Typography variant="h4" gutterBottom>
              Meu Dashboard
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Visão personalizada do sistema de folha de pagamento
            </Typography>
          </Box>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {!editMode && (
              <Button variant="outlined" onClick={enterEditMode}>
                Editar layout
              </Button>
            )}
            {editMode && (
              <>
                <Button variant="contained" onClick={() => void handleSave()} disabled={saving || !dirty}>
                  Salvar
                </Button>
                <Button variant="outlined" onClick={cancelEdit} disabled={saving}>
                  Cancelar
                </Button>
                <Button variant="outlined" color="warning" onClick={() => setResetDialogOpen(true)} disabled={saving}>
                  Restaurar padrão
                </Button>
                <Button variant="contained" onClick={() => setCatalogOpen(true)}>
                  Adicionar widget
                </Button>
              </>
            )}
          </Stack>
        </Box>

        {widgets.length === 0 ? (
          <DashboardEmptyState editMode={editMode} onAddWidgets={() => setCatalogOpen(true)} />
        ) : (
          <DashboardGrid
            widgets={widgets}
            stats={stats}
            editMode={editMode}
            onWidgetsChange={editMode ? updateDraftWidgets : undefined}
            onRemoveWidget={editMode ? handleRemoveWidget : undefined}
          />
        )}
      </Box>

      <WidgetCatalogDrawer
        open={catalogOpen}
        onClose={() => setCatalogOpen(false)}
        catalog={catalog}
        widgets={widgets}
        onAddWidget={handleAddWidget}
        onLimitReached={(message) => showNotification(message, 'warning')}
      />

      <Dialog open={resetDialogOpen} onClose={() => setResetDialogOpen(false)} aria-labelledby="reset-dialog-title">
        <DialogTitle id="reset-dialog-title">Restaurar layout padrão?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Esta ação substitui seu layout personalizado pelos 11 widgets padrão. Deseja continuar?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResetDialogOpen(false)}>Cancelar</Button>
          <Button color="warning" onClick={() => void handleReset()} autoFocus>
            Restaurar
          </Button>
        </DialogActions>
      </Dialog>

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}
