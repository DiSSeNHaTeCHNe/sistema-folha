import type { WidgetConfig, WidgetInstance } from './types';
import {
  DIMENSAO_OPTIONS,
  getConfigFieldsForWidget,
  METRICA_OPTIONS,
  TOP_N_MAX,
  TOP_N_MIN,
  TIPO_VISUALIZACAO_OPTIONS,
} from './widgetConfigOptions';

const COMPETENCIA_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

export interface ConfigValidationResult {
  valid: boolean;
  errors: string[];
}

function validateField(widgetId: string, config: WidgetConfig): string[] {
  const errors: string[] = [];
  const allowed = new Set(getConfigFieldsForWidget(widgetId));

  if (config.topN != null) {
    if (!allowed.has('topN')) {
      errors.push('topN não permitido para este widget');
    } else if (!Number.isInteger(config.topN) || config.topN < TOP_N_MIN || config.topN > TOP_N_MAX) {
      errors.push(`topN deve estar entre ${TOP_N_MIN} e ${TOP_N_MAX}`);
    }
  }

  if (config.competencia != null && config.competencia !== '') {
    if (!allowed.has('competencia')) {
      errors.push('competencia não permitida para este widget');
    } else if (!COMPETENCIA_PATTERN.test(config.competencia)) {
      errors.push('competencia deve estar no formato yyyy-MM');
    }
  }

  if (config.dimensao != null) {
    if (!allowed.has('dimensao')) {
      errors.push('dimensao não permitida para este widget');
    } else if (!DIMENSAO_OPTIONS.some((o) => o.value === config.dimensao)) {
      errors.push('dimensao inválida');
    }
  }

  if (config.metrica != null) {
    if (!allowed.has('metrica')) {
      errors.push('metrica não permitida para este widget');
    } else if (!METRICA_OPTIONS.some((o) => o.value === config.metrica)) {
      errors.push('metrica inválida');
    }
  }

  if (config.tipoVisualizacao != null) {
    if (!allowed.has('tipoVisualizacao')) {
      errors.push('tipoVisualizacao não permitido para este widget');
    } else if (!TIPO_VISUALIZACAO_OPTIONS.some((o) => o.value === config.tipoVisualizacao)) {
      errors.push('tipoVisualizacao inválido');
    }
  }

  if (config.centroCustoId != null) {
    if (!allowed.has('centroCustoId')) {
      errors.push('centroCustoId não permitido para este widget');
    }
  }

  if (config.linhaNegocioId != null) {
    if (!allowed.has('linhaNegocioId')) {
      errors.push('linhaNegocioId não permitido para este widget');
    }
  }

  return errors;
}

export function validateWidgetConfig(
  widgetId: string,
  config: WidgetConfig | null | undefined,
): ConfigValidationResult {
  if (!config || Object.keys(config).length === 0) {
    return { valid: true, errors: [] };
  }
  const errors = validateField(widgetId, config);
  return { valid: errors.length === 0, errors };
}

export function validateLayoutConfigs(widgets: WidgetInstance[]): ConfigValidationResult {
  const errors: string[] = [];
  for (const widget of widgets) {
    const result = validateWidgetConfig(widget.widgetId, widget.config);
    if (!result.valid) {
      errors.push(...result.errors.map((e) => `${widget.instanceId}: ${e}`));
    }
  }
  return { valid: errors.length === 0, errors };
}
