import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { DashboardEmptyState, WidgetCatalogDrawer } from './WidgetCatalogDrawer';
import { renderWithProviders } from '../../test/renderWithProviders';
import type { WidgetCatalogItem, WidgetInstance } from './types';
import { MAX_WIDGETS } from './types';

const catalog: WidgetCatalogItem[] = [
  {
    widgetId: 'kpi-total-funcionarios',
    titulo: 'Total de Funcionários',
    descricao: 'KPI',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
  },
  {
    widgetId: 'kpi-custo-empresa',
    titulo: 'Custo Empresa',
    descricao: 'KPI',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
  },
];

const widgets: WidgetInstance[] = [
  { widgetId: 'kpi-total-funcionarios', instanceId: 'a1', ordem: 0, colSpan: 3, rowSpan: 1 },
];

describe('WidgetCatalogDrawer', () => {
  it('marks already added widgets as unavailable', () => {
    renderWithProviders(
      <WidgetCatalogDrawer
        open
        onClose={() => {}}
        catalog={catalog}
        widgets={widgets}
        onAddWidget={() => {}}
      />,
    );
    expect(screen.getByText('Já adicionado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Adicionar Custo Empresa' })).not.toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByRole('button', { name: 'Adicionar Total de Funcionários' })).toHaveAttribute('aria-disabled', 'true');
  });

  it('adds widget via callback', () => {
    const onAddWidget = vi.fn();
    renderWithProviders(
      <WidgetCatalogDrawer
        open
        onClose={() => {}}
        catalog={catalog}
        widgets={widgets}
        onAddWidget={onAddWidget}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar Custo Empresa' }));
    expect(onAddWidget).toHaveBeenCalledWith(catalog[1]);
  });

  it('blocks add when max widgets reached', () => {
    const onLimitReached = vi.fn();
    const fullWidgets = Array.from({ length: MAX_WIDGETS }, (_, index) => ({
      widgetId: `widget-${index}`,
      instanceId: `id-${index}`,
      ordem: index,
      colSpan: 3,
      rowSpan: 1,
    }));
    renderWithProviders(
      <WidgetCatalogDrawer
        open
        onClose={() => {}}
        catalog={catalog}
        widgets={fullWidgets}
        onAddWidget={() => {}}
        onLimitReached={onLimitReached}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar Custo Empresa' }));
    expect(onLimitReached).toHaveBeenCalledWith('Limite de 30 widgets atingido');
    expect(screen.getByText('Limite de 30 widgets atingido')).toBeInTheDocument();
  });
});

describe('DashboardEmptyState', () => {
  it('shows CTA in edit mode', () => {
    const onAddWidgets = vi.fn();
    renderWithProviders(<DashboardEmptyState editMode onAddWidgets={onAddWidgets} />);
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar widgets' }));
    expect(onAddWidgets).toHaveBeenCalled();
  });
});
