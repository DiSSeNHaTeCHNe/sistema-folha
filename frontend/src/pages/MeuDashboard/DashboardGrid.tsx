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
import { Box, IconButton, ToggleButton, ToggleButtonGroup } from '@mui/material';
import type { WidgetInstance } from './types';
import { COL_SPAN_PRESETS, type ColSpanPreset } from './types';
import { WidgetFrame } from './WidgetFrame';
import { WidgetConfigPanel } from './WidgetConfigPanel';
import { WidgetDataRenderer } from './WidgetDataRenderer';
import { validateWidgetConfig } from './widgetConfigValidation';
import { getWidgetDefinition } from './widgets/registry';

interface DashboardGridProps {
  widgets: WidgetInstance[];
  competenciaGlobal: string | null;
  editMode: boolean;
  onWidgetsChange?: (widgets: WidgetInstance[]) => void;
  onRemoveWidget?: (instanceId: string) => void;
}

function normalizeOrder(widgets: WidgetInstance[]): WidgetInstance[] {
  return widgets.map((widget, index) => ({ ...widget, ordem: index }));
}

function SortableWidgetItem({
  instance,
  competenciaGlobal,
  editMode,
  onColSpanChange,
  onConfigChange,
  onRemoveWidget,
}: {
  instance: WidgetInstance;
  competenciaGlobal: string | null;
  editMode: boolean;
  onColSpanChange: (instanceId: string, colSpan: number) => void;
  onConfigChange?: (instanceId: string, config: WidgetInstance['config']) => void;
  onRemoveWidget?: (instanceId: string) => void;
}) {
  const definition = getWidgetDefinition(instance.widgetId);
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

  const configValidation = validateWidgetConfig(instance.widgetId, instance.config);

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
        {editMode && onConfigChange && (
          <WidgetConfigPanel
            widgetId={instance.widgetId}
            config={instance.config}
            validationErrors={configValidation.valid ? [] : configValidation.errors}
            onChange={(config) => onConfigChange(instance.instanceId, config)}
          />
        )}
        <WidgetDataRenderer
          instance={instance}
          competenciaGlobal={competenciaGlobal}
          editMode={editMode}
          definition={definition}
        />
      </WidgetFrame>
    </Box>
  );
}

export function DashboardGrid({
  widgets,
  competenciaGlobal,
  editMode,
  onWidgetsChange,
  onRemoveWidget,
}: DashboardGridProps) {
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const sorted = [...widgets].sort((a, b) => a.ordem - b.ordem);
  const ids = sorted.map((widget) => widget.instanceId);

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id || !onWidgetsChange) {
      return;
    }
    const oldIndex = sorted.findIndex((widget) => widget.instanceId === active.id);
    const newIndex = sorted.findIndex((widget) => widget.instanceId === over.id);
    if (oldIndex < 0 || newIndex < 0) {
      return;
    }
    onWidgetsChange(normalizeOrder(arrayMove(sorted, oldIndex, newIndex)));
  };

  const handleColSpanChange = (instanceId: string, colSpan: number) => {
    if (!onWidgetsChange) {
      return;
    }
    onWidgetsChange(
      sorted.map((widget) => (widget.instanceId === instanceId ? { ...widget, colSpan } : widget)),
    );
  };

  const handleConfigChange = (instanceId: string, config: WidgetInstance['config']) => {
    if (!onWidgetsChange) {
      return;
    }
    onWidgetsChange(
      sorted.map((widget) => (widget.instanceId === instanceId ? { ...widget, config } : widget)),
    );
  };

  const grid = (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(12, 1fr)',
        gap: 3,
      }}
    >
      {sorted.map((instance) => (
        <SortableWidgetItem
          key={instance.instanceId}
          instance={instance}
          competenciaGlobal={competenciaGlobal}
          editMode={editMode}
          onColSpanChange={handleColSpanChange}
          onConfigChange={editMode ? handleConfigChange : undefined}
          onRemoveWidget={onRemoveWidget}
        />
      ))}
    </Box>
  );

  if (!editMode) {
    return grid;
  }

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={ids} strategy={rectSortingStrategy}>
        {grid}
      </SortableContext>
    </DndContext>
  );
}
