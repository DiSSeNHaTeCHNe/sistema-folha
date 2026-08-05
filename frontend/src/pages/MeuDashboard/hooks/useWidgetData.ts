import { useQuery } from '@tanstack/react-query';
import {
  buildWidgetQueryParams,
  getWidgetData,
} from '../../../services/dashboardWidgetService';
import type { WidgetConfig } from '../types';

const STALE_TIME_MS = 5 * 60 * 1000;

export function widgetDataQueryKey(
  widgetId: string,
  instanceId: string,
  config: WidgetConfig | null | undefined,
  competenciaGlobal: string | null,
) {
  const resolvedCompetencia = config?.competencia ?? competenciaGlobal ?? null;
  return ['widget-data', widgetId, instanceId, config ?? {}, resolvedCompetencia] as const;
}

export interface UseWidgetDataOptions {
  enabled?: boolean;
}

export function useWidgetData(
  widgetId: string,
  instanceId: string,
  config: WidgetConfig | null | undefined,
  competenciaGlobal: string | null,
  options: UseWidgetDataOptions = {},
) {
  const { enabled = true } = options;
  const queryParams = buildWidgetQueryParams(config, competenciaGlobal);

  return useQuery({
    queryKey: widgetDataQueryKey(widgetId, instanceId, config, competenciaGlobal),
    queryFn: () => getWidgetData(widgetId, queryParams),
    staleTime: STALE_TIME_MS,
    enabled: enabled && Boolean(widgetId),
  });
}
