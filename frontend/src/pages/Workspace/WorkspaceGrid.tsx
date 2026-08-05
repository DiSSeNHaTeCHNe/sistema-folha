import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  type DragEndEvent,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  rectSortingStrategy,
  sortableKeyboardCoordinates,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { Alert, Box, IconButton, ToggleButton, ToggleButtonGroup } from '@mui/material';
import type { UserWidgetDefinition, WorkspaceLayoutWidget } from './types';
import { COL_SPAN_PRESETS, type ColSpanPreset } from './types';
import { WidgetFrame } from '../MeuDashboard/WidgetFrame';
import { WidgetDataRenderer } from './widgets/WidgetDataRenderer';
import { getWorkspaceWidgetDefinition } from './widgets/registry';
import { SourceBadge, resolveWidgetSource } from './components/SourceBadge';

interface WorkspaceGridProps {
  workspaceId: number;
  widgets: WorkspaceLayoutWidget[];
  userDefinitions: UserWidgetDefinition[];
  competencia?: string | null;
  editMode: boolean;
  onWidgetsChange?: (widgets: WorkspaceLayoutWidget[]) => void;
  onRemoveWidget?: (instanceId: string) => void;
}

function normalizeOrder(widgets: WorkspaceLayoutWidget[]): WorkspaceLayoutWidget[] {
  return widgets.map((widget, index) => ({ ...widget, ordem: index }));
}

function SortableWidgetItem({
  workspaceId,
  instance,
  userDefinitions,
  competencia,
  editMode,
  onColSpanChange,
  onRemoveWidget,
}: {
  workspaceId: number;
  instance: WorkspaceLayoutWidget;
  userDefinitions: UserWidgetDefinition[];
  competencia?: string | null;
  editMode: boolean;
  onColSpanChange: (instanceId: string, colSpan: number) => void;
  onRemoveWidget?: (instanceId: string) => void;
}) {
  const userMap = new Map(
    userDefinitions.map((item) => [item.id, { nome: item.nome, tipo: item.tipo, invalido: item.invalido }]),
  );
  const definition = getWorkspaceWidgetDefinition(instance, userMap);
  const userDef = instance.userWidgetDefinitionId
    ? userDefinitions.find((item) => item.id === instance.userWidgetDefinitionId)
    : undefined;
  const invalid = userDef?.invalido ?? false;
  const sourceKind = resolveWidgetSource(instance, userDefinitions);

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: instance.instanceId,
    disabled: !editMode,
  });

  if (!definition) {
    return null;
  }

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.85 : 1,
  };

  const presetValue = (Object.entries(COL_SPAN_PRESETS).find(([, span]) => span === instance.colSpan)?.[0] ??
    'M') as ColSpanPreset;

  return (
    <Box
      ref={setNodeRef}
      style={style}
      sx={{
        gridColumn: { xs: 'span 12', md: `span ${instance.colSpan}` },
        gridRow: { md: `span ${instance.rowSpan}` },
      }}
    >
      <WidgetFrame
        title={definition.titulo}
        editMode={editMode}
        badge={<SourceBadge source={sourceKind} />}
        toolbar={
          editMode ? (
            <Box display="flex" alignItems="center" gap={1} mb={1}>
              <IconButton aria-label={`Reordenar ${definition.titulo}`} size="small" {...attributes} {...listeners}>
                <DragIndicatorIcon fontSize="small" />
              </IconButton>
              <ToggleButtonGroup
                exclusive
                size="small"
                value={presetValue}
                aria-label={`Largura do widget ${definition.titulo}`}
                onChange={(_event, value: ColSpanPreset | null) => {
                  if (value) {
                    onColSpanChange(instance.instanceId, COL_SPAN_PRESETS[value]);
                  }
                }}
              >
                {(Object.keys(COL_SPAN_PRESETS) as ColSpanPreset[]).map((preset) => (
                  <ToggleButton key={preset} value={preset} aria-label={`Largura ${preset}`}>
                    {preset}
                  </ToggleButton>
                ))}
              </ToggleButtonGroup>
              <IconButton
                aria-label={`Remover ${definition.titulo}`}
                size="small"
                onClick={() => onRemoveWidget?.(instance.instanceId)}
              >
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            </Box>
          ) : undefined
        }
      >
        {invalid && (
          <Alert severity="warning" role="alert" sx={{ mb: 1 }}>
            Fórmula inválida neste widget — revise a definição antes de confiar nos números.
          </Alert>
        )}
        <WidgetDataRenderer
          workspaceId={workspaceId}
          widget={instance}
          userDefinitions={userDefinitions}
          competencia={competencia}
          editMode={editMode}
        />
      </WidgetFrame>
    </Box>
  );
}

export function WorkspaceGrid({
  workspaceId,
  widgets,
  userDefinitions,
  competencia = null,
  editMode,
  onWidgetsChange,
  onRemoveWidget,
}: WorkspaceGridProps) {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    if (!editMode || !onWidgetsChange) {
      return;
    }
    const { active, over } = event;
    if (!over || active.id === over.id) {
      return;
    }
    const oldIndex = widgets.findIndex((widget) => widget.instanceId === active.id);
    const newIndex = widgets.findIndex((widget) => widget.instanceId === over.id);
    if (oldIndex < 0 || newIndex < 0) {
      return;
    }
    onWidgetsChange(normalizeOrder(arrayMove(widgets, oldIndex, newIndex)));
  };

  const handleColSpanChange = (instanceId: string, colSpan: number) => {
    if (!onWidgetsChange) {
      return;
    }
    onWidgetsChange(widgets.map((widget) => (widget.instanceId === instanceId ? { ...widget, colSpan } : widget)));
  };

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={widgets.map((widget) => widget.instanceId)} strategy={rectSortingStrategy}>
        <Box
          display="grid"
          gridTemplateColumns={{ xs: 'repeat(12, 1fr)', md: 'repeat(12, 1fr)' }}
          gap={2}
          aria-label="Grid de widgets do workspace"
        >
          {widgets.map((instance) => (
            <SortableWidgetItem
              key={instance.instanceId}
              workspaceId={workspaceId}
              instance={instance}
              userDefinitions={userDefinitions}
              competencia={competencia}
              editMode={editMode}
              onColSpanChange={handleColSpanChange}
              onRemoveWidget={onRemoveWidget}
            />
          ))}
        </Box>
      </SortableContext>
    </DndContext>
  );
}
