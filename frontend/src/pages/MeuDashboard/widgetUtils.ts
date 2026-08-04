import type { WidgetCatalogItem, WidgetInstance } from './types';

export function gerarInstanceId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID().replace(/-/g, '').slice(0, 8);
  }
  return Math.random().toString(16).slice(2, 10);
}

export function criarWidgetFromCatalog(item: WidgetCatalogItem, ordem: number): WidgetInstance {
  return {
    widgetId: item.widgetId,
    instanceId: gerarInstanceId(),
    ordem,
    colSpan: item.colSpanPadrao,
    rowSpan: item.rowSpanPadrao,
  };
}
