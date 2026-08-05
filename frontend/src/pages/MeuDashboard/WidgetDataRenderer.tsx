import { Alert, Box, Button, CircularProgress, Typography } from '@mui/material';
import { useWidgetData } from './hooks/useWidgetData';
import type { WidgetInstance } from './types';
import type { WidgetDefinition } from './widgets/registry';

interface WidgetDataRendererProps {
  instance: WidgetInstance;
  competenciaGlobal: string | null;
  editMode: boolean;
  definition: WidgetDefinition;
}

export function WidgetDataRenderer({
  instance,
  competenciaGlobal,
  editMode,
  definition,
}: WidgetDataRendererProps) {
  const { data, isLoading, isError, refetch } = useWidgetData(
    instance.widgetId,
    instance.instanceId,
    instance.config,
    competenciaGlobal,
  );

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={120}>
        <CircularProgress size={32} aria-label={`Carregando ${definition.titulo}`} />
      </Box>
    );
  }

  if (isError) {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={() => void refetch()}>
            Recarregar
          </Button>
        }
      >
        Erro ao carregar dados do widget
      </Alert>
    );
  }

  if (!data || data.semDados) {
    return (
      <Box
        display="flex"
        alignItems="center"
        justifyContent="center"
        minHeight={120}
        role="status"
        aria-label={`Sem dados para ${definition.titulo}`}
      >
        <Typography color="text.secondary">
          Sem dados para a competência selecionada
        </Typography>
      </Box>
    );
  }

  const { Component } = definition;
  return <Component instance={instance} data={data} editMode={editMode} />;
}
