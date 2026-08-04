import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import OrganogramaGrafico from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('reactflow', () => ({
  default: ({
    nodes,
    nodeTypes,
    children,
  }: {
    nodes: Array<{ id: string; type: string; data: Record<string, unknown> }>;
    nodeTypes: Record<string, React.ComponentType<{ data: Record<string, unknown> }>>;
    children: ReactNode;
  }) => (
    <div role="region" aria-label="organograma-grafico">
      {nodes?.map((node) => {
        const NodeComponent = nodeTypes[node.type];
        return NodeComponent ? <NodeComponent key={node.id} data={node.data} /> : null;
      })}
      {children}
    </div>
  ),
  Background: () => null,
  Controls: () => null,
  MiniMap: () => null,
  Panel: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Handle: () => null,
  Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
  MarkerType: { ArrowClosed: 'arrowclosed' },
  useNodesState: (initial: unknown[]) => {
    const state = [...initial];
    return [state, vi.fn(), vi.fn()];
  },
  useEdgesState: (initial: unknown[]) => {
    const state = [...initial];
    return [state, vi.fn(), vi.fn()];
  },
}));

const makeNo = (overrides: Record<string, unknown> = {}) => ({
  id: 1,
  nome: 'Diretoria',
  descricao: 'Raiz',
  nivel: 0,
  posicao: 0,
  ativo: true,
  parentId: undefined,
  funcionarios: [{ id: 10, nome: 'Maria', cpf: '123', ativo: true }],
  centrosCusto: [{ id: 1, descricao: 'TI', codigo: 'CC1' }],
  children: [],
  ...overrides,
});

const sampleNo = makeNo({
  children: [
    makeNo({
      id: 2,
      nome: 'Gerência',
      descricao: 'Filho',
      parentId: 1,
      funcionarios: [],
      centrosCusto: [],
      children: [],
    }),
  ],
});

const manyFuncionariosNo = makeNo({
  funcionarios: Array.from({ length: 5 }, (_, i) => ({
    id: i + 1,
    nome: `Func ${i + 1}`,
    cpf: `${i}`,
    ativo: true,
  })),
  centrosCusto: Array.from({ length: 4 }, (_, i) => ({
    id: i + 1,
    descricao: `CC ${i + 1}`,
    codigo: `C${i}`,
  })),
});

describe('OrganogramaGrafico', () => {
  const onEdit = vi.fn();
  const onDelete = vi.fn();
  const onAddChild = vi.fn();
  const onRemoveFuncionario = vi.fn();
  const onRemoveCentroCusto = vi.fn();
  const onToggleExpand = vi.fn();
  const onHover = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  function renderGraph(props: Partial<React.ComponentProps<typeof OrganogramaGrafico>> = {}) {
    return renderWithProviders(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={onEdit}
        onDelete={onDelete}
        onAddChild={onAddChild}
        onRemoveFuncionario={onRemoveFuncionario}
        onRemoveCentroCusto={onRemoveCentroCusto}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={onToggleExpand}
        onHover={onHover}
        {...props}
      />,
    );
  }

  it('renders reactflow region with zoom hint', () => {
    renderGraph();
    expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
    expect(screen.getByText(/Use o scroll do mouse para zoom/)).toBeInTheDocument();
  });

  it('renders empty graph when no nodes', () => {
    renderWithProviders(
      <OrganogramaGrafico
        nos={[]}
        onEdit={onEdit}
        onDelete={onDelete}
        onAddChild={onAddChild}
        onRemoveFuncionario={onRemoveFuncionario}
        onRemoveCentroCusto={onRemoveCentroCusto}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={onToggleExpand}
        onHover={onHover}
      />,
    );
    expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
  });

  it('renders node names in compact mode', () => {
    renderGraph();
    expect(screen.getByText('Diretoria')).toBeInTheDocument();
    expect(screen.getByText('Gerência')).toBeInTheDocument();
  });

  it('calls onToggleExpand when node card is clicked', () => {
    renderGraph();
    fireEvent.click(screen.getByText('Diretoria'));
    expect(onToggleExpand).toHaveBeenCalledWith(1);
  });

  it('calls onHover on mouse enter and leave', () => {
    renderGraph();
    const card = screen.getByText('Diretoria').closest('.MuiCard-root')!;
    fireEvent.mouseEnter(card);
    expect(onHover).toHaveBeenCalledWith(1);
    fireEvent.mouseLeave(card);
    expect(onHover).toHaveBeenCalledWith(null);
  });

  it('calls onEdit from compact mode button', () => {
    renderGraph();
    fireEvent.click(screen.getAllByTitle('Editar')[0]);
    expect(onEdit).toHaveBeenCalled();
  });

  it('calls onDelete from compact mode button', () => {
    renderGraph();
    fireEvent.click(screen.getAllByTitle('Excluir')[0]);
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('calls onAddChild from compact mode button', () => {
    renderGraph();
    fireEvent.click(screen.getAllByTitle('Adicionar filho')[0]);
    expect(onAddChild).toHaveBeenCalledWith(1);
  });

  it('shows expanded details when expandedNodeId matches', () => {
    renderGraph({ expandedNodeId: 1 });
    expect(screen.getByText('Raiz')).toBeInTheDocument();
    expect(screen.getByText('Maria')).toBeInTheDocument();
  });

  it('shows expanded details on hover', () => {
    renderGraph({ hoveredNodeId: 1 });
    expect(screen.getByText('Raiz')).toBeInTheDocument();
  });

  it('removes funcionario from expanded node', () => {
    renderGraph({ expandedNodeId: 1 });
    const chip = screen.getByText('Maria').closest('.MuiChip-root')!;
    fireEvent.click(chip.querySelector('.MuiChip-deleteIcon') as HTMLElement);
    expect(onRemoveFuncionario).toHaveBeenCalledWith(1, 10);
  });

  it('removes centro de custo from expanded node', () => {
    renderGraph({ expandedNodeId: 1 });
    const chip = screen.getByText('TI').closest('.MuiChip-root')!;
    fireEvent.click(chip.querySelector('.MuiChip-deleteIcon') as HTMLElement);
    expect(onRemoveCentroCusto).toHaveBeenCalledWith(1, 1);
  });

  it('shows overflow chips when many funcionarios and centros', () => {
    renderGraph({ nos: [manyFuncionariosNo], expandedNodeId: 1 });
    expect(screen.getAllByText('+2').length).toBeGreaterThanOrEqual(1);
  });

  it('updates when expandedNodeId changes', async () => {
    const { rerender } = renderGraph();
    rerender(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={onEdit}
        onDelete={onDelete}
        onAddChild={onAddChild}
        onRemoveFuncionario={onRemoveFuncionario}
        onRemoveCentroCusto={onRemoveCentroCusto}
        expandedNodeId={1}
        hoveredNodeId={null}
        onToggleExpand={onToggleExpand}
        onHover={onHover}
      />,
    );
    await waitFor(() => expect(screen.getByText('Raiz')).toBeInTheDocument());
  });

  it('renders node without descricao in expanded mode', () => {
    renderGraph({ nos: [makeNo({ descricao: undefined })], expandedNodeId: 1 });
    expect(screen.getByText('Diretoria')).toBeInTheDocument();
    expect(screen.queryByText('Raiz')).not.toBeInTheDocument();
  });

  it('calls expanded mode edit button', () => {
    renderGraph({ expandedNodeId: 1 });
    fireEvent.click(screen.getAllByRole('button').find((b) => b.querySelector('[data-testid="EditIcon"]'))!);
    expect(onEdit).toHaveBeenCalled();
  });

  it('renders under indigo dark theme', () => {
    renderWithProviders(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={onEdit}
        onDelete={onDelete}
        onAddChild={onAddChild}
        onRemoveFuncionario={onRemoveFuncionario}
        onRemoveCentroCusto={onRemoveCentroCusto}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={onToggleExpand}
        onHover={onHover}
      />,
      { temaId: 'indigo' },
    );
    expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
    expect(screen.getByText('Diretoria')).toBeInTheDocument();
  });
});
