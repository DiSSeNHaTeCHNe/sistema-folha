import { useEffect, useMemo, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { DashboardGrid } from './DashboardGrid';
import { DashboardEmptyState, WidgetCatalogDrawer } from './WidgetCatalogDrawer';
import { useDashboardLayout } from './hooks/useDashboardLayout';
import type { WidgetCatalogItem, WidgetInstance } from './types';
import { criarWidgetFromCatalog } from './widgetUtils';

export default function MeuDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const { notification, showNotification, hideNotification } = useNotification();
  const {
    activeLayout,
    catalog,
    editMode,
    loading: layoutLoading,
    enterEditMode,
    updateDraftWidgets,
    setDraftLayout,
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
          <Stack direction="row" spacing={1}>
            {!editMode && (
              <Button variant="outlined" onClick={enterEditMode}>
                Editar layout
              </Button>
            )}
            {editMode && (
              <Button variant="contained" onClick={() => setCatalogOpen(true)}>
                Adicionar widget
              </Button>
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

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}
