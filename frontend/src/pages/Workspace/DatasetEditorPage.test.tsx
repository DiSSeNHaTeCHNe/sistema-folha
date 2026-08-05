import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import DatasetEditorPage from './DatasetEditorPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  createDatasetRow,
  deleteDatasetRow,
  getDataset,
  listDatasetRows,
  listDatasets,
  updateDatasetRow,
  updateDatasetSchema,
  WorkspaceApiError,
} from '../../services/workspaceService';
import { colors } from './workspaceTheme';

vi.mock('../../services/workspaceService', () => ({
  getDataset: vi.fn(),
  listDatasetRows: vi.fn(),
  listDatasets: vi.fn(),
  updateDatasetSchema: vi.fn(),
  createDatasetRow: vi.fn(),
  updateDatasetRow: vi.fn(),
  deleteDatasetRow: vi.fn(),
  WorkspaceApiError: class WorkspaceApiError extends Error {
    status: number;
    errors?: { field: string; message: string }[];
    constructor(status: number, message: string, errors?: { field: string; message: string }[]) {
      super(message);
      this.status = status;
      this.errors = errors;
    }
  },
}));

const dataset = {
  id: 1,
  nome: 'Previsão',
  campos: [
    { nome: 'headcount', tipo: 'NUMERO' as const },
    { nome: 'custo', tipo: 'MOEDA' as const },
  ],
  schemaVersion: 1,
  totalLinhas: 1,
};

const rows = [{ id: 10, datasetId: 1, valores: { headcount: 5, custo: '1000.50' }, ordem: 0 }];

function renderEditor() {
  return renderWithProviders(<DatasetEditorPage datasetId={1} />);
}

describe('DatasetEditorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getDataset).mockResolvedValue(dataset);
    vi.mocked(listDatasetRows).mockResolvedValue(rows);
    vi.mocked(listDatasets).mockResolvedValue([dataset, { id: 2, nome: 'Outro', schemaVersion: 1, totalLinhas: 0, totalCampos: 1 }]);
    vi.mocked(updateDatasetSchema).mockImplementation(async (_id, campos, version) => ({
      ...dataset,
      campos,
      schemaVersion: version + 1,
    }));
    vi.mocked(updateDatasetRow).mockImplementation(async (_dsId, rowId, valores) => ({
      id: rowId,
      datasetId: 1,
      valores,
      ordem: 0,
    }));
    vi.mocked(createDatasetRow).mockResolvedValue({ id: 11, datasetId: 1, valores: {}, ordem: 1 });
    vi.mocked(deleteDatasetRow).mockResolvedValue(undefined);
  });

  it('loads and displays dataset name', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('heading', { name: /Editor: Previsão/i })).toBeInTheDocument());
  });

  it('renders schema fields by name with spec column headers (WKS2-12)', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByDisplayValue('headcount')).toBeInTheDocument());
    expect(screen.getByDisplayValue('custo')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Campo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Tipo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Obrigatório' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Observação' })).toBeInTheDocument();
  });

  it('renders row values after switching to Linhas tab', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Linhas' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Linhas' }));
    await waitFor(() => expect(screen.getByDisplayValue('5')).toBeInTheDocument());
    expect(screen.getByDisplayValue('1000.50')).toBeInTheDocument();
  });

  it('adds a new schema field row', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('button', { name: /Adicionar campo/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Adicionar campo/i }));
    expect(screen.getAllByLabelText('Campo')).toHaveLength(3);
  });

  it('shows field type panel with Referência description (WKS2-13)', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByText(/Tipos de campo/i)).toBeInTheDocument());
    expect(screen.getByText(/Vincula a entidades do sistema/i)).toBeInTheDocument();
  });

  it('shows dataset and row quota bars (WKS2-14)', async () => {
    renderEditor();
    await waitFor(() =>
      expect(screen.getByRole('progressbar', { name: 'Datasets do usuário: 2 de 20' })).toBeInTheDocument(),
    );
    expect(screen.getByRole('progressbar', { name: 'Linhas deste dataset: 1 de 500' })).toBeInTheDocument();
  });

  it('saves schema via API', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar esquema' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Salvar esquema' }));
    await waitFor(() => expect(updateDatasetSchema).toHaveBeenCalled());
  });

  it('shows confirm dialog on 409 schema conflict', async () => {
    vi.mocked(updateDatasetSchema).mockRejectedValue(new WorkspaceApiError(409, 'Campo com dados'));
    renderEditor();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar esquema' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Salvar esquema' }));
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Remover campos com dados/i })).toBeInTheDocument(),
    );
  });

  it('confirms destructive schema change', async () => {
    vi.mocked(updateDatasetSchema)
      .mockRejectedValueOnce(new WorkspaceApiError(409, 'Campo com dados'))
      .mockResolvedValueOnce({ ...dataset, schemaVersion: 2, campos: [{ nome: 'custo', tipo: 'MOEDA' }] });
    renderEditor();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar esquema' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Salvar esquema' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar remoção' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar remoção' }));
    await waitFor(() => expect(updateDatasetSchema).toHaveBeenLastCalledWith(1, expect.any(Array), 1, true));
  });

  it('shows field-level inline error on invalid row save (WKS2-15)', async () => {
    vi.mocked(updateDatasetRow).mockRejectedValue(
      new WorkspaceApiError(400, 'Validação', [{ field: 'valores.headcount', message: 'Esperado número' }]),
    );
    renderEditor();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Linhas' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Linhas' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar linha' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Salvar linha' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Esperado número'));
  });

  it('adds a new data row', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Linhas' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Linhas' }));
    await waitFor(() => expect(screen.getByRole('button', { name: /Adicionar linha/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Adicionar linha/i }));
    await waitFor(() => expect(createDatasetRow).toHaveBeenCalled());
  });

  it('deletes a row', async () => {
    renderEditor();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Linhas' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Linhas' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Excluir linha 10' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Excluir linha 10' }));
    await waitFor(() => expect(deleteDatasetRow).toHaveBeenCalledWith(1, 10));
  });

  it('persists observacao on save and reload (WKS2F1-09/10)', async () => {
    vi.mocked(updateDatasetSchema).mockImplementation(async (_id, campos, version) => ({
      ...dataset,
      campos,
      schemaVersion: version + 1,
    }));

    renderEditor();
    await waitFor(() => expect(screen.getByDisplayValue('headcount')).toBeInTheDocument());

    fireEvent.change(screen.getAllByLabelText('Observação')[0], {
      target: { value: 'Total de colaboradores' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar esquema' }));

    await waitFor(() =>
      expect(updateDatasetSchema).toHaveBeenCalledWith(
        1,
        expect.arrayContaining([
          expect.objectContaining({ nome: 'headcount', observacao: 'Total de colaboradores' }),
        ]),
        1,
        false,
      ),
    );

    await waitFor(() =>
      expect(screen.getByDisplayValue('Total de colaboradores')).toBeInTheDocument(),
    );
  });

  it('reloads saved observacao when dataset already has value (WKS2F1-10)', async () => {
    vi.mocked(getDataset).mockResolvedValue({
      ...dataset,
      campos: [{ nome: 'headcount', tipo: 'NUMERO', observacao: 'Valor salvo' }],
    });

    renderEditor();
    await waitFor(() => expect(screen.getByDisplayValue('Valor salvo')).toBeInTheDocument());
  });

  it('shows not found when dataset missing', async () => {
    vi.mocked(getDataset).mockRejectedValue(new Error('404'));
    renderEditor();
    await waitFor(() => expect(screen.getByText(/Dataset não encontrado/i)).toBeInTheDocument());
  });

  it('shows loading status initially', () => {
    vi.mocked(getDataset).mockReturnValue(new Promise(() => {}));
    renderEditor();
    expect(screen.getByRole('status')).toHaveTextContent(/Carregando dataset/i);
  });

  it('applies Techne page and navy tokens from workspaceTheme (WKS2F1-16)', async () => {
    const { container } = renderEditor();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Editor: Previsão/i, level: 1 })).toBeInTheDocument(),
    );
    const shell = container.firstElementChild as HTMLElement;
    expect(shell).toHaveStyle({ backgroundColor: colors.page });
    expect(screen.getByRole('heading', { name: /Editor: Previsão/i, level: 1 })).toHaveStyle({
      color: colors.navy,
    });
  });
});
