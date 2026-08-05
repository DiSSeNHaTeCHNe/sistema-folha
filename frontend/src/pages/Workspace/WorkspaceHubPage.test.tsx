import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import WorkspaceHubPage from './WorkspaceHubPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  createWorkspace,
  listDatasets,
  listWidgetDefinitions,
  listWorkspaces,
} from '../../services/workspaceService';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../services/workspaceService', () => ({
  listWorkspaces: vi.fn(),
  listDatasets: vi.fn(),
  listWidgetDefinitions: vi.fn(),
  createWorkspace: vi.fn(),
}));

const workspaces = [
  { id: 1, nome: 'Planejamento', totalWidgets: 3 },
  { id: 2, nome: 'RH', totalWidgets: 1 },
];

const datasets = [
  { id: 10, nome: 'Headcount', schemaVersion: 1, totalLinhas: 12, totalCampos: 4 },
  { id: 11, nome: 'Custos', schemaVersion: 2, totalLinhas: 5, totalCampos: 3 },
];

describe('WorkspaceHubPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockReset();
    vi.mocked(listWorkspaces).mockResolvedValue(workspaces);
    vi.mocked(listDatasets).mockResolvedValue(datasets);
    vi.mocked(listWidgetDefinitions).mockResolvedValue([
      {
        id: 1,
        nome: 'KPI HC',
        tipo: 'KPI',
        fontes: [{ kind: 'DATASET', ref: '10' }],
        formula: 'SOMA(x)',
        config: {},
        invalido: false,
      },
    ]);
    vi.mocked(createWorkspace).mockResolvedValue({ id: 3, nome: 'Novo', widgets: [] });
  });

  it('renders workspace cards with name, widget count and last edit (WKS2-01)', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Planejamento', level: 2 })).toBeInTheDocument());
    expect(screen.getByText('3 widgets')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'RH', level: 2 })).toBeInTheDocument();
    expect(screen.getAllByText(/Última edição/i)).toHaveLength(2);
  });

  it('shows Novo workspace action and opens create dialog (WKS2-01)', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo workspace' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo workspace' }));
    expect(screen.getByRole('heading', { name: 'Novo workspace', level: 2 })).toBeInTheDocument();
  });

  it('renders datasets summary table with spec columns (WKS2-02)', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getByRole('cell', { name: 'Headcount' })).toBeInTheDocument());
    expect(screen.getByRole('columnheader', { name: 'Campos' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Linhas' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Usado por' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Publicado' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Última alteração' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '4' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '1 widget' })).toBeInTheDocument();
  });

  it('shows dataset quota progress bar (WKS2-04)', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() =>
      expect(screen.getByRole('progressbar', { name: 'Datasets: 2 de 20' })).toBeInTheDocument(),
    );
  });

  it('shows partial error when datasets fail but workspaces load (edge case)', async () => {
    vi.mocked(listDatasets).mockRejectedValue(new Error('fail'));
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Planejamento', level: 2 })).toBeInTheDocument());
    expect(screen.getByRole('alert')).toHaveTextContent(/Erro ao carregar datasets/i);
  });

  it('shows empty state when no workspaces exist', async () => {
    vi.mocked(listWorkspaces).mockResolvedValue([]);
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() =>
      expect(screen.getByRole('status', { name: 'Nenhum workspace configurado' })).toBeInTheDocument(),
    );
  });

  it('navigates to workspace detail when Abrir is clicked (WKS2-03)', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getAllByRole('button', { name: 'Abrir' })).toHaveLength(2));
    fireEvent.click(screen.getAllByRole('button', { name: 'Abrir' })[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/1');
  });

  it('shows Novo dataset action in datasets section', async () => {
    renderWithProviders(<WorkspaceHubPage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo dataset' })).toBeInTheDocument());
  });
});
