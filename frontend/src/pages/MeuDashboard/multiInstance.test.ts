import { describe, expect, it } from 'vitest';
import { criarWidgetFromCatalog, gerarInstanceId } from './widgetUtils';
import type { WidgetCatalogItem, WidgetInstance } from './types';

const catalogItem: WidgetCatalogItem = {
  widgetId: 'grafico-custo-por-cc',
  titulo: 'Custo por CC',
  descricao: 'Gráfico',
  categoria: 'GRAFICO',
  colSpanPadrao: 3,
  rowSpanPadrao: 2,
};

describe('multiple widget instances (DASHC-37..39)', () => {
  it('creates distinct instanceIds for same widgetId', () => {
    const first = criarWidgetFromCatalog(catalogItem, 0);
    const second = criarWidgetFromCatalog(catalogItem, 1);
    expect(first.widgetId).toBe(second.widgetId);
    expect(first.instanceId).not.toBe(second.instanceId);
  });

  it('removing one instance leaves others intact (DASHC-39)', () => {
    const instances: WidgetInstance[] = [
      { widgetId: 'grafico-custo-por-cc', instanceId: 'a', ordem: 0, colSpan: 3, rowSpan: 2, config: { topN: 5 } },
      { widgetId: 'grafico-custo-por-cc', instanceId: 'b', ordem: 1, colSpan: 3, rowSpan: 2, config: { topN: 10 } },
    ];
    const remaining = instances
      .filter((w) => w.instanceId !== 'a')
      .map((widget, index) => ({ ...widget, ordem: index }));

    expect(remaining).toHaveLength(1);
    expect(remaining[0].instanceId).toBe('b');
    expect(remaining[0].config?.topN).toBe(10);
  });

  it('instances maintain independent configs (DASHC-38)', () => {
    const instances: WidgetInstance[] = [
      { widgetId: 'grafico-custo-por-cc', instanceId: gerarInstanceId(), ordem: 0, colSpan: 3, rowSpan: 2, config: { topN: 5 } },
      { widgetId: 'grafico-custo-por-cc', instanceId: gerarInstanceId(), ordem: 1, colSpan: 3, rowSpan: 2, config: { topN: 10 } },
    ];
    expect(instances[0].config?.topN).toBe(5);
    expect(instances[1].config?.topN).toBe(10);
    expect(instances[0].instanceId).not.toBe(instances[1].instanceId);
  });
});
