import { useEffect, useMemo, useState } from 'react';
import { Alert, Box, CircularProgress, Typography } from '@mui/material';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';
import { getDashboardLayout } from '../../services/dashboardLayoutService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import type { DashboardLayout, WidgetInstance } from './types';
import { WidgetFrame } from './WidgetFrame';
import { getWidgetDefinition } from './widgets/registry';

function sortWidgets(widgets: WidgetInstance[]): WidgetInstance[] {
  return [...widgets].sort((a, b) => a.ordem - b.ordem);
}

function StaticDashboardGrid({
  widgets,
  stats,
}: {
  widgets: WidgetInstance[];
  stats: DashboardStats;
}) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(12, 1fr)',
        gap: 3,
        '@media (max-width: 900px)': {
          '& > *': { gridColumn: 'span 12 !important' },
        },
      }}
    >
      {widgets.map((instance) => {
        const definition = getWidgetDefinition(instance.widgetId);
        if (!definition) {
          return null;
        }
        const { Component } = definition;
        return (
          <Box
            key={instance.instanceId}
            sx={{ gridColumn: `span ${instance.colSpan}`, gridRow: `span ${instance.rowSpan}` }}
          >
            <WidgetFrame title={definition.titulo}>
              <Component instance={instance} stats={stats} editMode={false} />
            </WidgetFrame>
          </Box>
        );
      })}
    </Box>
  );
}

export default function MeuDashboard() {
  const [layout, setLayout] = useState<DashboardLayout | null>(null);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { notification, showNotification, hideNotification } = useNotification();

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        const [layoutData, statsData] = await Promise.all([getDashboardLayout(), getDashboardStats()]);
        if (!cancelled) {
          setLayout(layoutData);
          setStats(statsData);
          setError(null);
        }
      } catch {
        if (!cancelled) {
          setError('Erro ao carregar Meu Dashboard');
          showNotification('Erro ao carregar Meu Dashboard', 'error');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [showNotification]);

  const sortedWidgets = useMemo(
    () => (layout ? sortWidgets(layout.widgets) : []),
    [layout],
  );

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} aria-label="Carregando Meu Dashboard" />
      </Box>
    );
  }

  if (error || !layout || !stats) {
    return <Alert severity="error">{error ?? 'Erro ao carregar Meu Dashboard'}</Alert>;
  }

  return (
    <>
      <Box sx={{ backgroundColor: 'background.default', minHeight: '100vh' }}>
        <Box mb={4}>
          <Typography variant="h4" gutterBottom>
            Meu Dashboard
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            Visão personalizada do sistema de folha de pagamento
          </Typography>
        </Box>
        <StaticDashboardGrid widgets={sortedWidgets} stats={stats} />
      </Box>
      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}
