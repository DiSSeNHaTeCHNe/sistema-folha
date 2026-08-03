import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
import Organograma from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { organogramaService } from '../../services/organogramaService';
import { funcionarioService } from '../../services/funcionarioService';
import { centroCustoService } from '../../services/centroCustoService';

const dndHandlers: {
  onDragStart?: (event: { active: { id: string } }) => void;
  onDragEnd?: (event: { active: { id: string }; over: { id: string } | null }) => void;
} = {};

vi.mock('@dnd-kit/core', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@dnd-kit/core')>();
  return {
    ...actual,
    DndContext: ({
      children,
      onDragStart,
      onDragEnd,
    }: {
      children: React.ReactNode;
      onDragStart?: (event: { active: { id: string } }) => void;
      onDragEnd?: (event: { active: { id: string }; over: { id: string } | null }) => void;
    }) => {
      dndHandlers.onDragStart = onDragStart;
      dndHandlers.onDragEnd = onDragEnd;
      return <div data-testid="dnd-context">{children}</div>;
    },
    DragOverlay: ({ children }: { children: React.ReactNode }) => (
      <div data-testid="drag-overlay">{children}</div>
    ),
    useDraggable: ({ id }: { id: string }) => ({
      attributes: {},
      listeners: {},
      setNodeRef: vi.fn(),
      transform: id === 'funcionario-11' ? { x: 4, y: 2 } : null,
      isDragging: id === 'funcionario-11',
    }),
    useDroppable: ({ id }: { id: string }) => ({
      setNodeRef: vi.fn(),
      isOver: id === 'no-1',
    }),
  };
});

const sampleFuncionario = { id: 10, nome: 'Maria Silva', cpf: '12345678901', ativo: true };
const sampleFuncionario2 = { id: 11, nome: 'João Santos', cpf: '98765432100', ativo: true };
const sampleCentro = { id: 1, descricao: 'TI', codigo: 'CC1', ativo: true, linhaNegocioId: 1 };
const sampleCentro2 = { id: 2, descricao: 'RH', codigo: 'CC2', ativo: true, linhaNegocioId: 1 };

const rootNo = {
  id: 1,
  nome: 'Diretoria',
  descricao: 'Nó raiz',
  nivel: 0,
  parentId: undefined,
  posicao: 0,
  ativo: true,
  funcionarioIds: [10],
  centroCustoIds: [1],
};

const childNo = {
  id: 2,
  nome: 'Gerência',
  descricao: 'Subnó',
  nivel: 1,
  parentId: 1,
  posicao: 0,
  ativo: true,
  funcionarioIds: [],
  centroCustoIds: [],
};

vi.mock('../../services/organogramaService', () => ({
  organogramaService: {
    listarTodos: vi.fn(),
    criarNo: vi.fn(),
    atualizarNo: vi.fn(),
    removerNo: vi.fn(),
    adicionarFuncionario: vi.fn(),
    removerFuncionario: vi.fn(),
    adicionarCentroCusto: vi.fn(),
    removerCentroCusto: vi.fn(),
  },
}));

vi.mock('../../services/funcionarioService', () => ({
  funcionarioService: { listar: vi.fn() },
}));

vi.mock('../../services/centroCustoService', () => ({
  centroCustoService: { listarTodos: vi.fn() },
}));

vi.mock('../../components/OrganogramaGrafico', () => ({
  default: ({
    onEdit,
    onDelete,
    onAddChild,
  }: {
    onEdit: (no: { id: number; nome: string }) => void;
    onDelete: (id: number) => void;
    onAddChild: (parentId: number) => void;
  }) => (
    <div role="region" aria-label="organograma-grafico-mock">
      <button type="button" onClick={() => onEdit({ id: 1, nome: 'Diretoria', descricao: '', nivel: 0, posicao: 0, ativo: true })}>
        Graph Edit
      </button>
      <button type="button" onClick={() => onDelete(1)}>Graph Delete</button>
      <button type="button" onClick={() => onAddChild(1)}>Graph Add Child</button>
    </div>
  ),
}));

vi.mock('react-toastify', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const childNo2 = {
  id: 3,
  nome: 'Operações',
  descricao: '',
  nivel: 1,
  parentId: 1,
  posicao: 1,
  ativo: true,
  funcionarioIds: [],
  centroCustoIds: [],
};

function setupMocks(nos = [rootNo, childNo, childNo2]) {
  vi.mocked(organogramaService.listarTodos).mockResolvedValue(nos);
  vi.mocked(funcionarioService.listar).mockResolvedValue([sampleFuncionario, sampleFuncionario2]);
  vi.mocked(centroCustoService.listarTodos).mockResolvedValue([sampleCentro, sampleCentro2]);
}

describe('Organograma page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupMocks();
  });

  it('renders the page heading without real HTTP', async () => {
    renderWithProviders(<Organograma />);
    expect(screen.getByRole('heading', { name: /Organograma/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('Carregando organograma...')).not.toBeInTheDocument();
    });
  });

  it('displays nodes in list view with hierarchy', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => {
      expect(screen.getByText('Diretoria')).toBeInTheDocument();
      expect(screen.getByText('Gerência')).toBeInTheDocument();
    });
  });

  it('shows empty state when no nodes exist', async () => {
    vi.mocked(organogramaService.listarTodos).mockResolvedValue([]);
    renderWithProviders(<Organograma />);
    await waitFor(() => {
      expect(screen.getByText(/Clique em "Novo Nó Raiz" para começar/)).toBeInTheDocument();
    });
  });

  it('shows toast error when loading fails', async () => {
    vi.mocked(organogramaService.listarTodos).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Organograma />);
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao carregar dados do organograma');
    });
  });

  it('switches to graph view and handles graph actions', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(organogramaService.removerNo).mockResolvedValue(undefined);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Gráfico' }));
    await waitFor(() => expect(screen.getByRole('region', { name: 'organograma-grafico-mock' })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Graph Edit' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Graph Add Child' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Graph Delete' }));
    await waitFor(() => expect(organogramaService.removerNo).toHaveBeenCalledWith(1));
  });

  it('shows graph empty state when no nodes', async () => {
    vi.mocked(organogramaService.listarTodos).mockResolvedValue([]);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Gráfico' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Gráfico' }));
    await waitFor(() => expect(screen.getByText('Nenhum nó criado')).toBeInTheDocument());
  });

  it('creates a root node', async () => {
    vi.mocked(organogramaService.criarNo).mockResolvedValue({ ...rootNo, id: 3 });

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo Nó Raiz' })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Novo Nó Raiz' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Novo Nó' } });
    fireEvent.change(within(dialog).getByLabelText('Descrição'), { target: { value: 'Desc' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Criar' }));

    await waitFor(() => {
      expect(organogramaService.criarNo).toHaveBeenCalledWith(
        expect.objectContaining({ nome: 'Novo Nó', descricao: 'Desc' }),
      );
      expect(toast.success).toHaveBeenCalledWith('Nó criado com sucesso');
    });
  });

  it('edits an existing node', async () => {
    vi.mocked(organogramaService.atualizarNo).mockResolvedValue(rootNo);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());

    fireEvent.click(screen.getAllByTitle('Editar')[0]);
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('heading', { name: 'Editar Nó' })).toBeInTheDocument();
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Diretoria Atualizada' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(organogramaService.atualizarNo).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Nó atualizado com sucesso');
    });
  });

  it('shows error when save fails', async () => {
    vi.mocked(organogramaService.criarNo).mockRejectedValue({ response: { data: { message: 'Erro API' } } });

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo Nó Raiz' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo Nó Raiz' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Teste' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro API'));
  });

  it('deletes node when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(organogramaService.removerNo).mockResolvedValue(undefined);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTitle('Excluir')[0]);

    await waitFor(() => {
      expect(organogramaService.removerNo).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Nó excluído com sucesso');
    });
  });

  it('does not delete node when cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTitle('Excluir')[0]);
    expect(organogramaService.removerNo).not.toHaveBeenCalled();
  });

  it('shows delete error toast', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(organogramaService.removerNo).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTitle('Excluir')[0]);

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao excluir nó'));
  });

  it('opens create child dialog from add button', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTitle('Adicionar filho')[0]);

    await waitFor(() => {
      expect(screen.getByText('Criar Novo Nó')).toBeInTheDocument();
      expect(screen.getByText(/filho do nó selecionado/)).toBeInTheDocument();
    });
  });

  it('expands node on click and shows details', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Diretoria'));
    await waitFor(() => {
      expect(screen.getByText('Nó raiz')).toBeInTheDocument();
      expect(screen.getByText('Maria Silva')).toBeInTheDocument();
      expect(screen.getByText('Fixado')).toBeInTheDocument();
    });
  });

  it('toggles expand off when clicking same node again', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());

    const card = screen.getByText('Diretoria').closest('.MuiCard-root')!;
    fireEvent.click(card);
    await waitFor(() => expect(screen.getByText('Fixado')).toBeInTheDocument());
    fireEvent.click(card);
    await waitFor(() => expect(screen.queryByText('Fixado')).not.toBeInTheDocument());
  });

  it('shows hover details without fixing', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Gerência')).toBeInTheDocument());

    fireEvent.mouseEnter(screen.getByText('Gerência').closest('.MuiCard-root')!);
    await waitFor(() => expect(screen.getByText('Subnó')).toBeInTheDocument());
    expect(screen.queryByText('Fixado')).not.toBeInTheDocument();
  });

  it('removes funcionario from node', async () => {
    vi.mocked(organogramaService.removerFuncionario).mockResolvedValue(undefined);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Diretoria'));

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument());
    const chip = screen.getByText('Maria Silva').closest('.MuiChip-root')!;
    const deleteBtn = chip.querySelector('.MuiChip-deleteIcon') as HTMLElement;
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(organogramaService.removerFuncionario).toHaveBeenCalledWith(1, 10);
      expect(toast.success).toHaveBeenCalledWith('Funcionário removido do nó');
    });
  });

  it('removes centro de custo from node', async () => {
    vi.mocked(organogramaService.removerCentroCusto).mockResolvedValue(undefined);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Diretoria'));

    await waitFor(() => expect(screen.getByText('Centros de Custo (1)')).toBeInTheDocument());
    const deletableChip = screen
      .getAllByText('TI')
      .map((el) => el.closest('.MuiChip-root'))
      .find((chip) => chip?.querySelector('.MuiChip-deleteIcon'));
    fireEvent.click(deletableChip!.querySelector('.MuiChip-deleteIcon') as HTMLElement);

    await waitFor(() => {
      expect(organogramaService.removerCentroCusto).toHaveBeenCalledWith(1, 1);
      expect(toast.success).toHaveBeenCalledWith('Centro de custo removido do nó');
    });
  });

  it('shows remove funcionario error', async () => {
    vi.mocked(organogramaService.removerFuncionario).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Diretoria'));
    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument());
    const chip = screen.getByText('Maria Silva').closest('.MuiChip-root')!;
    fireEvent.click(chip.querySelector('.MuiChip-deleteIcon') as HTMLElement);

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao remover funcionário'));
  });

  it('filters funcionarios sidebar', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByPlaceholderText('Filtrar por nome...')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Filtrar por nome...'), { target: { value: 'João' } });
    await waitFor(() => {
      expect(screen.getByText('João Santos')).toBeInTheDocument();
      expect(screen.queryByText('Maria Silva')).not.toBeInTheDocument();
    });
  });

  it('filters centros de custo sidebar', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByPlaceholderText('Filtrar por descrição...')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Filtrar por descrição...'), { target: { value: 'RH' } });
    await waitFor(() => expect(screen.getByText('RH')).toBeInTheDocument());
  });

  it('shows all funcionarios associated message when none available', async () => {
    setupMocks([{ ...rootNo, funcionarioIds: [10, 11] }]);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Todos associados')).toBeInTheDocument());
  });

  it('shows no centros found when filter empty', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByPlaceholderText('Filtrar por descrição...')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText('Filtrar por descrição...'), { target: { value: 'xyz' } });
    await waitFor(() => expect(screen.getByText('Nenhum centro de custo encontrado')).toBeInTheDocument());
  });

  it('switches back to list view', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Gráfico' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Gráfico' }));
    fireEvent.click(screen.getByRole('button', { name: 'Lista' }));
    await waitFor(() => expect(screen.getByText('Estrutura do Organograma')).toBeInTheDocument());
  });

  it('uses fallback error message on save', async () => {
    vi.mocked(organogramaService.criarNo).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo Nó Raiz' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo Nó Raiz' }));
    fireEvent.change(within(screen.getByRole('dialog')).getByLabelText('Nome'), { target: { value: 'X' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('fail'));
  });

  it('handles nodes without funcionarioIds or centroCustoIds', async () => {
    setupMocks([{ ...rootNo, funcionarioIds: undefined, centroCustoIds: undefined }]);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
  });

  it('adds funcionario to node via drag and drop', async () => {
    vi.mocked(organogramaService.adicionarFuncionario).mockResolvedValue({} as never);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('João Santos')).toBeInTheDocument());

    dndHandlers.onDragStart?.({ active: { id: 'funcionario-11' } });
    await waitFor(() => {
      expect(screen.getByTestId('drag-overlay')).toHaveTextContent('João Santos');
    });

    dndHandlers.onDragEnd?.({ active: { id: 'funcionario-11' }, over: { id: 'no-1' } });

    await waitFor(() => {
      expect(organogramaService.adicionarFuncionario).toHaveBeenCalledWith(1, 11);
      expect(toast.success).toHaveBeenCalledWith('Funcionário adicionado ao nó');
    });
  });

  it('adds centro de custo to node via drag and drop', async () => {
    vi.mocked(organogramaService.adicionarCentroCusto).mockResolvedValue({} as never);

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('RH')).toBeInTheDocument());

    dndHandlers.onDragStart?.({ active: { id: 'centroCusto-2' } });
    dndHandlers.onDragEnd?.({ active: { id: 'centroCusto-2' }, over: { id: 'no-1' } });

    await waitFor(() => {
      expect(organogramaService.adicionarCentroCusto).toHaveBeenCalledWith(1, 2);
      expect(toast.success).toHaveBeenCalledWith('Centro de custo adicionado ao nó');
    });
  });

  it('ignores drag end without drop target', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    dndHandlers.onDragEnd?.({ active: { id: 'funcionario-11' }, over: null });
    expect(organogramaService.adicionarFuncionario).not.toHaveBeenCalled();
  });

  it('shows error when adding funcionario via drag fails', async () => {
    vi.mocked(organogramaService.adicionarFuncionario).mockRejectedValue({ response: { data: { message: 'Erro drag' } } });

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('João Santos')).toBeInTheDocument());
    dndHandlers.onDragEnd?.({ active: { id: 'funcionario-11' }, over: { id: 'no-1' } });

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro drag'));
  });

  it('shows error when adding centro via drag fails', async () => {
    vi.mocked(organogramaService.adicionarCentroCusto).mockRejectedValue(new Error('centro fail'));

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('RH')).toBeInTheDocument());
    dndHandlers.onDragEnd?.({ active: { id: 'centroCusto-2' }, over: { id: 'no-1' } });

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('centro fail'));
  });

  it('uses fallback error message when save has no details', async () => {
    vi.mocked(organogramaService.criarNo).mockRejectedValue('unknown');

    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo Nó Raiz' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo Nó Raiz' }));
    fireEvent.change(within(screen.getByRole('dialog')).getByLabelText('Nome'), { target: { value: 'X' } });
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao salvar nó'));
  });

  it('builds tree when parent id is missing from map', async () => {
    setupMocks([{ ...childNo, parentId: 999 }]);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.queryByText('Carregando organograma...')).not.toBeInTheDocument());
    expect(screen.queryByText('Gerência')).not.toBeInTheDocument();
  });

  it('shows no funcionarios found when filter matches nothing', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByPlaceholderText('Filtrar por nome...')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText('Filtrar por nome...'), { target: { value: 'Inexistente' } });
    await waitFor(() => expect(screen.getByText('Nenhum funcionário encontrado')).toBeInTheDocument());
  });

  it('shows drag overlay for centro de custo item', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('RH')).toBeInTheDocument());
    dndHandlers.onDragStart?.({ active: { id: 'centroCusto-2' } });
    await waitFor(() => expect(screen.getByTestId('drag-overlay')).toHaveTextContent('RH'));
  });

  it('ignores unrecognized drag combination', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    dndHandlers.onDragEnd?.({ active: { id: 'funcionario-11' }, over: { id: 'funcionario-10' } });
    expect(organogramaService.adicionarFuncionario).not.toHaveBeenCalled();
  });

  it('ignores drag start for unknown funcionario id', async () => {
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Diretoria')).toBeInTheDocument());
    dndHandlers.onDragStart?.({ active: { id: 'funcionario-999' } });
    expect(screen.getByTestId('drag-overlay')).toBeEmptyDOMElement();
  });

  it('updates node without descricao', async () => {
    vi.mocked(organogramaService.atualizarNo).mockResolvedValue(rootNo);
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('Operações')).toBeInTheDocument());
    fireEvent.click(screen.getAllByTitle('Editar').find((btn) => btn.closest('.MuiCard-root')?.textContent?.includes('Operações'))!);
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Descrição'), { target: { value: '' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(organogramaService.atualizarNo).toHaveBeenCalled());
  });

  it('uses fallback when drag error has response without message', async () => {
    vi.mocked(organogramaService.adicionarFuncionario).mockRejectedValue({ response: { data: {} } });
    renderWithProviders(<Organograma />);
    await waitFor(() => expect(screen.getByText('João Santos')).toBeInTheDocument());
    dndHandlers.onDragEnd?.({ active: { id: 'funcionario-11' }, over: { id: 'no-1' } });
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Erro ao adicionar funcionário'));
  });
});
