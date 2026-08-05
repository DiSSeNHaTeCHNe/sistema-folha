import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import WidgetBuilderPage from './WidgetBuilderPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  createWidgetDefinition,
  getWorkspace,
  listDatasets,
  previewWidgetDefinition,
  saveWorkspaceLayout,
  validateFormula,
  WorkspaceApiError,
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
  listDatasets: vi.fn(),
  validateFormula: vi.fn(),
  previewWidgetDefinition: vi.fn(),
  createWidgetDefinition: vi.fn(),
  getWorkspace: vi.fn(),
  saveWorkspaceLayout: vi.fn(),
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

const previewData = {
  instanceId: 'preview',
  userWidgetDefinitionId: null,
  widgetId: null,
  tipo: 'KPI',
  semDados: false,
  invalido: false,
  competencia: null,
  valores: { total: '1500,00' },
  linhas: [],
};

function renderBuilder(route = '/workspace/1/widgets/novo') {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/:workspaceId/widgets/novo" element={<WidgetBuilderPage />} />
    </Routes>,
    { route },
  );
}

describe('WidgetBuilderPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockReset();
    vi.mocked(listDatasets).mockResolvedValue([
      { id: 10, nome: 'Vendas', schemaVersion: 1, totalLinhas: 5, totalCampos: 2 },
    ]);
    vi.mocked(validateFormula).mockResolvedValue({ valid: true, errors: [] });
    vi.mocked(previewWidgetDefinition).mockResolvedValue(previewData);
    vi.mocked(createWidgetDefinition).mockResolvedValue({
      id: 99,
      nome: 'KPI Vendas',
      tipo: 'KPI',
      fontes: [{ kind: 'DATASET', ref: '10' }],
      formula: 'SOMA(valor)',
      config: {},
      invalido: false,
    });
    vi.mocked(getWorkspace).mockResolvedValue({
      id: 1,
      nome: 'Principal',
      widgets: [],
    });
    vi.mocked(saveWorkspaceLayout).mockResolvedValue({
      id: 1,
      nome: 'Principal',
      widgets: [{ instanceId: 'w-new', ordem: 0, colSpan: 4, rowSpan: 1, userWidgetDefinitionId: 99 }],
    });
  });

  it('renders builder form with tipo and fonte fields (WKS2-17)', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Novo widget', level: 1 })).toBeInTheDocument());
    expect(screen.getByLabelText('Nome')).toBeInTheDocument();
    expect(screen.getByLabelText('Tipo')).toBeInTheDocument();
    expect(screen.getByLabelText('Fonte de dado')).toBeInTheDocument();
    expect(screen.getByLabelText('Fórmula')).toBeInTheDocument();
  });

  it('loads ACL-filtered datasets as fonte options', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fonte de dado')).toBeInTheDocument());
    fireEvent.mouseDown(screen.getByLabelText('Fonte de dado'));
    await waitFor(() => expect(screen.getByRole('option', { name: 'Dataset: Vendas' })).toBeInTheDocument());
    expect(screen.getByRole('option', { name: 'Sistema: Orçamento' })).toBeInTheDocument();
  });

  it('debounces formula validation via validateFormula API (WKS2-18)', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fórmula')).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Vendas' } });
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(valor)' } });
    await waitFor(
      () =>
        expect(validateFormula).toHaveBeenCalledWith('SOMA(valor)', [
          { kind: 'DATASET', ref: '10' },
        ]),
      { timeout: 800 },
    );
  });

  it('shows inline formula error before submit (WKS2-18)', async () => {
    vi.mocked(validateFormula).mockResolvedValue({ valid: false, errors: ['Campo desconhecido'] });
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fórmula')).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(x)' } });
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Campo desconhecido'), { timeout: 800 });
  });

  it('shows preview with formatted KPI when formula is valid (WKS2-19)', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fórmula')).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Vendas' } });
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(valor)' } });
    await waitFor(() => expect(previewWidgetDefinition).toHaveBeenCalled(), { timeout: 800 });
    await waitFor(() => expect(screen.getByText('KPI Vendas')).toBeInTheDocument());
  });

  it('persists definition and adds widget to workspace layout (WKS2-20)', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fórmula')).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Vendas' } });
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(valor)' } });
    await waitFor(() => expect(previewWidgetDefinition).toHaveBeenCalled(), { timeout: 800 });
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar ao workspace' }));
    await waitFor(() => expect(createWidgetDefinition).toHaveBeenCalled());
    expect(saveWorkspaceLayout).toHaveBeenCalledWith(
      1,
      expect.arrayContaining([
        expect.objectContaining({ userWidgetDefinitionId: 99, ordem: 0 }),
      ]),
    );
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/1');
  });

  it('disables confirm until nome and valid formula', async () => {
    renderBuilder();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar ao workspace' })).toBeDisabled());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Vendas' } });
    expect(screen.getByRole('button', { name: 'Adicionar ao workspace' })).toBeDisabled();
  });

  it('shows error for invalid workspace id', () => {
    renderBuilder('/workspace/abc/widgets/novo');
    expect(screen.getByText('Identificador de workspace inválido.')).toBeInTheDocument();
  });

  it('shows submit error from API', async () => {
    vi.mocked(createWidgetDefinition).mockRejectedValue(
      new WorkspaceApiError(400, 'Erro', [{ field: 'formula', message: 'Função inválida' }]),
    );
    renderBuilder();
    await waitFor(() => expect(screen.getByLabelText('Fórmula')).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Vendas' } });
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(valor)' } });
    await waitFor(() => expect(previewWidgetDefinition).toHaveBeenCalled(), { timeout: 800 });
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar ao workspace' }));
    await waitFor(() => expect(screen.getByText('Função inválida')).toBeInTheDocument());
  });
});
