import { describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import OrganogramaGrafico from './index';
import { renderWithProviders } from '../../test/renderWithProviders';

vi.mock('reactflow', () => ({
  default: ({ children }: { children: ReactNode }) => (
    <div role="region" aria-label="organograma-grafico">{children}</div>
  ),
  Background: () => null,
  Controls: () => null,
  MiniMap: () => null,
  Panel: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Handle: () => null,
  Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
  MarkerType: { ArrowClosed: 'arrowclosed' },
  useNodesState: (initial: unknown[]) => [initial, vi.fn(), vi.fn()],
  useEdgesState: (initial: unknown[]) => [initial, vi.fn(), vi.fn()],
}));

const sampleNo = {
  id: 1,
  descricao: 'Diretoria',
  paiId: null,
  funcionarios: [{ id: 10, nome: 'Maria', cpf: '123', ativo: true }],
  centrosCusto: [{ id: 1, descricao: 'TI', codigo: 'CC1' }],
  children: [
    {
      id: 2,
      descricao: 'Gerência',
      paiId: 1,
      funcionarios: [],
      centrosCusto: [],
      children: [],
    },
  ],
};

const noop = vi.fn();

describe('OrganogramaGrafico', () => {
  it('renders reactflow region with zoom hint', () => {
    renderWithProviders(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={noop}
        onDelete={noop}
        onAddChild={noop}
        onRemoveFuncionario={noop}
        onRemoveCentroCusto={noop}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={noop}
        onHover={noop}
      />,
    );

    expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
    expect(screen.getByText(/Use o scroll do mouse para zoom/)).toBeInTheDocument();
  });

  it('renders empty graph when no nodes', () => {
    renderWithProviders(
      <OrganogramaGrafico
        nos={[]}
        onEdit={noop}
        onDelete={noop}
        onAddChild={noop}
        onRemoveFuncionario={noop}
        onRemoveCentroCusto={noop}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={noop}
        onHover={noop}
      />,
    );

    expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
  });

  it('updates when expandedNodeId changes', async () => {
    const { rerender } = renderWithProviders(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={noop}
        onDelete={noop}
        onAddChild={noop}
        onRemoveFuncionario={noop}
        onRemoveCentroCusto={noop}
        expandedNodeId={null}
        hoveredNodeId={null}
        onToggleExpand={noop}
        onHover={noop}
      />,
    );

    rerender(
      <OrganogramaGrafico
        nos={[sampleNo]}
        onEdit={noop}
        onDelete={noop}
        onAddChild={noop}
        onRemoveFuncionario={noop}
        onRemoveCentroCusto={noop}
        expandedNodeId={1}
        hoveredNodeId={null}
        onToggleExpand={noop}
        onHover={noop}
      />,
    );

    await waitFor(() => {
      expect(screen.getByRole('region', { name: 'organograma-grafico' })).toBeInTheDocument();
    });
  });
});
