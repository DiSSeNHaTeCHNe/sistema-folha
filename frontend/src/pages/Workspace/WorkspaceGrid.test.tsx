import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { WorkspaceGrid } from './WorkspaceGrid';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getWorkspaceWidgetData } from '../../services/workspaceService';
import type { WorkspaceLayoutWidget } from './types';

vi.mock('../../services/workspaceService', () => ({
  getWorkspaceWidgetData: vi.fn(),
}));

vi.mock('../../MeuDashboard/WidgetDataRenderer', () => ({
  WidgetDataRenderer: ({ definition }: { definition: { titulo: string } }) => (
    <div>catalog: {definition.titulo}</div>
  ),
}));

vi.mock('@dnd-kit/core', async () => {
  const actual = await vi.importActual<typeof import('@dnd-kit/core')>('@dnd-kit/core');
  return {
    ...actual,
    DndContext: ({
      children,
      onDragEnd,
    }: {
      children: React.ReactNode;
      onDragEnd: (event: { active: { id: string }; over: { id: string } }) => void;
    }) => (
      <div>
        {children}
        <button
          type="button"
          onClick={() => onDragEnd({ active: { id: 'w1' }, over: { id: 'w2' } })}
        >
          Simular reordenação
        </button>
      </div>
    ),
  };
});

const catalogWidget: WorkspaceLayoutWidget = {
  instanceId: 'w1',
  ordem: 0,
  colSpan: 3,
  rowSpan: 1,
  widgetId: 'kpi-total-funcionarios',
};

const userWidget: WorkspaceLayoutWidget = {
  instanceId: 'w2',
  ordem: 1,
  colSpan: 4,
  rowSpan: 1,
  userWidgetDefinitionId: 7,
};

const userDefinitions = [
  {
    id: 7,
    nome: 'KPI Custom',
    tipo: 'KPI' as const,
    fontes: [{ kind: 'DATASET' as const, ref: '1' }],
    formula: 'SOMA(x)',
    config: {},
    invalido: false,
  },
];

function renderGrid(
  widgets: WorkspaceLayoutWidget[],
  options: {
    editMode?: boolean;
    onWidgetsChange?: (widgets: WorkspaceLayoutWidget[]) => void;
    onRemoveWidget?: (id: string) => void;
  } = {},
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return renderWithProviders(
    <QueryClientProvider client={queryClient}>
      <WorkspaceGrid
        workspaceId={1}
        widgets={widgets}
        userDefinitions={userDefinitions}
        competencia="2026-06"
        editMode={options.editMode ?? false}
        onWidgetsChange={options.onWidgetsChange}
        onRemoveWidget={options.onRemoveWidget}
      />
    </QueryClientProvider>,
  );
}

describe('WorkspaceGrid', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getWorkspaceWidgetData).mockResolvedValue({
      instanceId: 'w2',
      userWidgetDefinitionId: 7,
      widgetId: null,
      tipo: 'KPI',
      semDados: false,
      invalido: false,
      competencia: '2026-06',
      valores: { total: '100.00' },
      linhas: [],
    });
  });

  it('renders grid landmark', () => {
    renderGrid([catalogWidget]);
    expect(screen.getByLabelText('Grid de widgets do workspace')).toBeInTheDocument();
  });

  it('renders catalog widget frame by accessible name', () => {
    renderGrid([catalogWidget]);
    expect(screen.getByLabelText('Total de Funcionários')).toBeInTheDocument();
  });

  it('renders user KPI widget data', async () => {
    renderGrid([userWidget]);
    await waitFor(() => expect(screen.getByText('R$ 100,00')).toBeInTheDocument());
  });

  it('shows invalid widget banner in edit mode', () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithProviders(
      <QueryClientProvider client={queryClient}>
        <WorkspaceGrid
          workspaceId={1}
          widgets={[userWidget]}
          userDefinitions={[{ ...userDefinitions[0], invalido: true }]}
          editMode
        />
      </QueryClientProvider>,
    );
    expect(screen.getAllByRole('alert').some((el) => el.textContent?.includes('Fórmula inválida'))).toBe(true);
  });

  it('reorders widgets in edit mode', () => {
    const onWidgetsChange = vi.fn();
    renderGrid([catalogWidget, userWidget], { editMode: true, onWidgetsChange });
    fireEvent.click(screen.getByRole('button', { name: 'Simular reordenação' }));
    expect(onWidgetsChange).toHaveBeenCalled();
    const next = onWidgetsChange.mock.calls[0][0] as WorkspaceLayoutWidget[];
    expect(next[0].instanceId).toBe('w2');
  });

  it('changes colSpan via preset toggle', () => {
    const onWidgetsChange = vi.fn();
    renderGrid([catalogWidget], { editMode: true, onWidgetsChange });
    fireEvent.click(screen.getByRole('button', { name: 'Largura G' }));
    expect(onWidgetsChange).toHaveBeenCalledWith([
      expect.objectContaining({ instanceId: 'w1', colSpan: 6 }),
    ]);
  });

  it('removes widget in edit mode', () => {
    const onRemoveWidget = vi.fn();
    renderGrid([catalogWidget], { editMode: true, onRemoveWidget });
    fireEvent.click(screen.getByRole('button', { name: /Remover Total de Funcionários/i }));
    expect(onRemoveWidget).toHaveBeenCalledWith('w1');
  });

  it('does not expose edit controls outside edit mode', () => {
    renderGrid([catalogWidget], { editMode: false });
    expect(screen.queryByRole('button', { name: /Reordenar/i })).not.toBeInTheDocument();
  });
});
