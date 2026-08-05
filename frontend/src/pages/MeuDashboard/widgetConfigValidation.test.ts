import { describe, expect, it } from 'vitest';
import { validateLayoutConfigs, validateWidgetConfig } from './widgetConfigValidation';

describe('widgetConfigValidation', () => {
  it('accepts valid topN within bounds', () => {
    const result = validateWidgetConfig('grafico-custo-por-cc', { topN: 10 });
    expect(result.valid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('rejects topN outside 1..50 (DASHC-35)', () => {
    const result = validateWidgetConfig('grafico-custo-por-cc', { topN: 99 });
    expect(result.valid).toBe(false);
    expect(result.errors[0]).toMatch(/topN deve estar entre 1 e 50/);
  });

  it('rejects invalid competencia format', () => {
    const result = validateWidgetConfig('kpi-total-funcionarios', { competencia: '06/2026' });
    expect(result.valid).toBe(false);
    expect(result.errors[0]).toMatch(/yyyy-MM/);
  });

  it('accepts centroCustoId and linhaNegocioId filter config (DASHC-33)', () => {
    const result = validateWidgetConfig('grafico-funcionarios-por-cc', {
      centroCustoId: 2,
      linhaNegocioId: 3,
    });
    expect(result.valid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('rejects invalid tipoVisualizacao', () => {
    const result = validateWidgetConfig('grafico-funcionarios-por-cc', {
      tipoVisualizacao: 'DONUT' as 'PIE',
    });
    expect(result.valid).toBe(false);
    expect(result.errors[0]).toMatch(/tipoVisualizacao inválido/);
  });

  it('validates all widgets in layout before save', () => {
    const result = validateLayoutConfigs([
      {
        widgetId: 'grafico-custo-por-cc',
        instanceId: 'inst-1',
        ordem: 0,
        colSpan: 3,
        rowSpan: 2,
        config: { topN: 100 },
      },
    ]);
    expect(result.valid).toBe(false);
    expect(result.errors[0]).toContain('inst-1');
  });
});
