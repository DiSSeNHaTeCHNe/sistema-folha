import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  getWorkspaceWidgetDefinition,
  resolveUserWidgetRegistryId,
  USER_WIDGET_PREFIX,
  WORKSPACE_WIDGET_REGISTRY,
} from './registry';
import { WidgetDataRenderer } from './WidgetDataRenderer';
import { renderWithProviders } from '../../../test/renderWithProviders';
import { getWorkspaceWidgetData } from '../../../services/workspaceService';

vi.mock('../../../services/workspaceService', () => ({
  getWorkspaceWidgetData: vi.fn(),
}));

vi.mock('../../MeuDashboard/WidgetDataRenderer', () => ({
  WidgetDataRenderer: () => <div>catalog-widget</div>,
}));

vi.mock('../../MeuDashboard/widgets/registry', async () => {
  const actual = await vi.importActual<typeof import('../../MeuDashboard/widgets/registry')>(
    '../../MeuDashboard/widgets/registry',
  );
  return actual;
});

describe('workspace widget registry', () => {
  it('includes USER_* entries for each dynamic tipo', () => {
    const userEntries = WORKSPACE_WIDGET_REGISTRY.filter((item) => item.id.startsWith(USER_WIDGET_PREFIX));
    expect(userEntries.map((item) => item.id)).toEqual([
      'USER_KPI',
      'USER_TABELA',
      'USER_GRAFICO_LINHA',
      'USER_GRAFICO_BARRA',
    ]);
  });

  it('includes catalog widgets from Nível 1 registry', () => {
    expect(WORKSPACE_WIDGET_REGISTRY.some((item) => item.catalogWidgetId === 'kpi-total-funcionarios')).toBe(true);
  });

  it('resolves user widget definition by id and tipo', () => {
    const def = getWorkspaceWidgetDefinition(
      { instanceId: 'x', ordem: 0, colSpan: 4, rowSpan: 1, userWidgetDefinitionId: 9 },
      new Map([[9, { nome: 'Meu KPI', tipo: 'KPI', invalido: false }]]),
    );
    expect(def?.titulo).toBe('Meu KPI');
    expect(def?.kind).toBe('KPI');
  });

  it('resolves catalog widget by widgetId', () => {
    const def = getWorkspaceWidgetDefinition(
      { instanceId: 'x', ordem: 0, colSpan: 4, rowSpan: 1, widgetId: 'kpi-total-funcionarios' },
      new Map(),
    );
    expect(def?.catalogWidgetId).toBe('kpi-total-funcionarios');
  });

  it('resolveUserWidgetRegistryId maps tipo to USER_* id', () => {
    expect(resolveUserWidgetRegistryId('TABELA')).toBe('USER_TABELA');
  });

  it('returns null for unknown layout widget', () => {
    expect(
      getWorkspaceWidgetDefinition({ instanceId: 'x', ordem: 0, colSpan: 4, rowSpan: 1 }, new Map()),
    ).toBeNull();
  });
});

describe('WidgetDataRenderer', () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  function renderRenderer(widget: Parameters<typeof WidgetDataRenderer>[0]['widget']) {
    return renderWithProviders(
      <QueryClientProvider client={queryClient}>
        <WidgetDataRenderer workspaceId={1} widget={widget} userDefinitions={[]} competencia="2026-06" />
      </QueryClientProvider>,
    );
  }

  it('delegates catalog widgets to dashboard renderer', () => {
    renderRenderer({
      instanceId: 'c1',
      ordem: 0,
      colSpan: 3,
      rowSpan: 1,
      widgetId: 'kpi-total-funcionarios',
    });
    expect(screen.getByText('catalog-widget')).toBeInTheDocument();
  });

  it('shows invalid formula banner for user widget', async () => {
    vi.mocked(getWorkspaceWidgetData).mockResolvedValue({
      instanceId: 'u1',
      userWidgetDefinitionId: 5,
      widgetId: null,
      tipo: 'KPI',
      semDados: false,
      invalido: true,
      competencia: '2026-06',
      valores: {},
      linhas: [],
    });
    renderWithProviders(
      <QueryClientProvider client={queryClient}>
        <WidgetDataRenderer
          workspaceId={1}
          widget={{ instanceId: 'u1', ordem: 0, colSpan: 3, rowSpan: 1, userWidgetDefinitionId: 5 }}
          userDefinitions={[{ id: 5, nome: 'Quebrado', tipo: 'KPI', fontes: [], formula: 'X', config: {}, invalido: true }]}
        />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/Fórmula inválida/i));
  });

  it('renders dynamic KPI data for user widget', async () => {
    vi.mocked(getWorkspaceWidgetData).mockResolvedValue({
      instanceId: 'u2',
      userWidgetDefinitionId: 6,
      widgetId: null,
      tipo: 'KPI',
      semDados: false,
      invalido: false,
      competencia: '2026-06',
      valores: { total: '999.99' },
      linhas: [],
    });
    renderWithProviders(
      <QueryClientProvider client={queryClient}>
        <WidgetDataRenderer
          workspaceId={1}
          widget={{ instanceId: 'u2', ordem: 0, colSpan: 3, rowSpan: 1, userWidgetDefinitionId: 6 }}
          userDefinitions={[{ id: 6, nome: 'Total', tipo: 'KPI', fontes: [], formula: null, config: {}, invalido: false }]}
        />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.getByText('R$ 999,99')).toBeInTheDocument());
  });
});
