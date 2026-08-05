import { Box, Button, CircularProgress, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getWorkspaceWidgetData } from '../../../services/workspaceService';
import type { UserWidgetDefinition, WorkspaceLayoutWidget } from '../types';
import { getWorkspaceWidgetDefinition } from './registry';
import { DynamicKpiWidget } from './DynamicKpiWidget';
import { DynamicTableWidget } from './DynamicTableWidget';
import { DynamicChartWidget } from './DynamicChartWidget';
import { WidgetDataRenderer as DashboardWidgetDataRenderer } from '../../MeuDashboard/WidgetDataRenderer';
import { getWidgetDefinition } from '../../MeuDashboard/widgets/registry';
import type { WidgetInstance } from '../../MeuDashboard/types';
import { WidgetErrorBanner } from '../components/WidgetErrorBanner';

interface WidgetDataRendererProps {
  workspaceId: number;
  widget: WorkspaceLayoutWidget;
  userDefinitions: UserWidgetDefinition[];
  competencia?: string | null;
  editMode?: boolean;
}

function toDashboardInstance(widget: WorkspaceLayoutWidget): WidgetInstance | null {
  if (!widget.widgetId) {
    return null;
  }
  return {
    widgetId: widget.widgetId,
    instanceId: widget.instanceId,
    ordem: widget.ordem,
    colSpan: widget.colSpan,
    rowSpan: widget.rowSpan,
    config: (widget.config ?? undefined) as WidgetInstance['config'],
  };
}

export function WidgetDataRenderer({
  workspaceId,
  widget,
  userDefinitions,
  competencia = null,
  editMode = false,
}: WidgetDataRendererProps) {
  const userMap = new Map(
    userDefinitions.map((item) => [item.id, { nome: item.nome, tipo: item.tipo, invalido: item.invalido }]),
  );
  const definition = getWorkspaceWidgetDefinition(widget, userMap);
  const userDef = widget.userWidgetDefinitionId
    ? userDefinitions.find((item) => item.id === widget.userWidgetDefinitionId)
    : undefined;

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['workspace-widget-data', workspaceId, widget.instanceId, competencia],
    queryFn: () => getWorkspaceWidgetData(workspaceId, widget.instanceId, competencia),
    enabled: widget.userWidgetDefinitionId != null || widget.widgetId != null,
  });

  if (widget.widgetId && getWidgetDefinition(widget.widgetId)) {
    const instance = toDashboardInstance(widget);
    const catalogDef = getWidgetDefinition(widget.widgetId);
    if (instance && catalogDef) {
      return (
        <DashboardWidgetDataRenderer
          instance={instance}
          competenciaGlobal={competencia}
          editMode={editMode}
          definition={catalogDef}
        />
      );
    }
  }

  if (userDef?.invalido || data?.invalido) {
    return (
      <WidgetErrorBanner
        variant="warn"
        title="Fórmula inválida"
        message={`Revise a definição do widget "${userDef?.nome ?? definition?.titulo}".`}
      />
    );
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={120}>
        <CircularProgress size={32} aria-label={`Carregando ${definition?.titulo ?? 'widget'}`} />
      </Box>
    );
  }

  if (isError) {
    return (
      <WidgetErrorBanner
        title="Erro ao carregar"
        message="Não foi possível carregar os dados deste widget."
        action={
          <Button color="inherit" size="small" onClick={() => void refetch()}>
            Recarregar
          </Button>
        }
      />
    );
  }

  if (!data || data.semDados) {
    return (
      <Box display="flex" alignItems="center" justifyContent="center" minHeight={120} role="status">
        <Typography color="text.secondary">Sem dados para exibir</Typography>
      </Box>
    );
  }

  if (!definition) {
    return <WidgetErrorBanner message="Widget desconhecido" />;
  }

  const title = userDef?.nome ?? definition.titulo;
  const tipo = userDef?.tipo ?? data.tipo;

  if (tipo === 'TABELA') {
    return <DynamicTableWidget title={title} data={data} />;
  }
  if (tipo === 'GRAFICO_LINHA') {
    return <DynamicChartWidget title={title} data={data} variant="GRAFICO_LINHA" />;
  }
  if (tipo === 'GRAFICO_BARRA') {
    return <DynamicChartWidget title={title} data={data} variant="GRAFICO_BARRA" />;
  }

  return <DynamicKpiWidget title={title} data={data} />;
}
