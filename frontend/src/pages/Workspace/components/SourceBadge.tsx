import { StatusChip } from './StatusChip';
import type { WidgetSourceKind } from '../types';

export interface SourceBadgeProps {
  source: WidgetSourceKind;
}

const SOURCE_LABELS: Record<WidgetSourceKind, string> = {
  DATASET: 'DATASET',
  SISTEMA: 'SISTEMA',
};

const SOURCE_VARIANTS = {
  DATASET: 'info',
  SISTEMA: 'ok',
} as const;

export function SourceBadge({ source }: SourceBadgeProps) {
  return <StatusChip variant={SOURCE_VARIANTS[source]} label={SOURCE_LABELS[source]} />;
}

export function resolveWidgetSource(
  widget: { widgetId?: string | null; userWidgetDefinitionId?: number | null },
  userDefinitions: { id: number; fontes: { kind: WidgetSourceKind }[] }[],
): WidgetSourceKind {
  if (widget.widgetId) {
    return 'SISTEMA';
  }
  const userDef = userDefinitions.find((item) => item.id === widget.userWidgetDefinitionId);
  if (userDef?.fontes.some((fonte) => fonte.kind === 'DATASET')) {
    return 'DATASET';
  }
  return 'SISTEMA';
}
