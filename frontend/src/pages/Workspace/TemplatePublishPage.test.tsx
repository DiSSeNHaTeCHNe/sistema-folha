import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import TemplatePublishPage from './TemplatePublishPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  getDataset,
  getWidgetDefinition,
  publishDatasetTemplate,
  publishWidgetTemplate,
} from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  getDataset: vi.fn(),
  getWidgetDefinition: vi.fn(),
  publishDatasetTemplate: vi.fn(),
  publishWidgetTemplate: vi.fn(),
  WorkspaceApiError: class WorkspaceApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
}));

function renderPublish(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/templates/publish" element={<TemplatePublishPage />} />
    </Routes>,
    { route },
  );
}

describe('TemplatePublishPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getDataset).mockResolvedValue({
      id: 5,
      nome: 'Vendas',
      campos: [{ nome: 'valor', tipo: 'MOEDA', obrigatorio: true }],
      schemaVersion: 1,
      totalLinhas: 10,
    });
    vi.mocked(getWidgetDefinition).mockResolvedValue({
      id: 7,
      nome: 'KPI Vendas',
      tipo: 'KPI',
      fontes: [{ kind: 'DATASET', ref: '5' }],
      formula: 'SOMA(valor)',
      config: {},
      invalido: false,
    });
    vi.mocked(publishDatasetTemplate).mockResolvedValue({
      id: 1,
      nome: 'Vendas',
      tipo: 'DATASET',
      versaoAtual: 1,
      estruturaHash: 'abc',
      novaVersaoCriada: true,
    });
    vi.mocked(publishWidgetTemplate).mockResolvedValue({
      id: 2,
      nome: 'KPI Vendas',
      tipo: 'WIDGET',
      versaoAtual: 1,
      estruturaHash: 'def',
      novaVersaoCriada: true,
    });
  });

  it('shows structure checklist for dataset publish (WKS2-22)', async () => {
    renderPublish('/workspace/templates/publish?datasetId=5');
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Será publicado', level: 2 })).toBeInTheDocument());
    expect(screen.getByText(/Esquema de campos/i)).toBeInTheDocument();
    expect(screen.getByText('Campo: valor (MOEDA)')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nunca será publicado', level: 2 })).toBeInTheDocument();
    expect(screen.getByText(/Linhas e valores preenchidos/i)).toBeInTheDocument();
  });

  it('shows structure checklist for widget publish (WKS2-22)', async () => {
    renderPublish('/workspace/templates/publish?widgetId=7');
    await waitFor(() => expect(screen.getByText(/Definição do widget/i)).toBeInTheDocument());
    expect(screen.getByText('Fórmula: SOMA(valor)')).toBeInTheDocument();
    expect(screen.getByText(/Histórico de auditoria/i)).toBeInTheDocument();
  });

  it('blocks publish when item not found or unsaved (WKS2-24)', async () => {
    vi.mocked(getDataset).mockRejectedValue(new Error('404'));
    renderPublish('/workspace/templates/publish?datasetId=999');
    await waitFor(() =>
      expect(screen.getByText(/Item não encontrado ou não salvo/i)).toBeInTheDocument(),
    );
    expect(screen.getByRole('button', { name: 'Confirmar publicação' })).toBeDisabled();
  });

  it('blocks publish without query params (WKS2-24)', async () => {
    renderPublish('/workspace/templates/publish');
    await waitFor(() =>
      expect(screen.getByText(/Informe um datasetId ou widgetId salvo/i)).toBeInTheDocument(),
    );
    expect(screen.getByRole('button', { name: 'Confirmar publicação' })).toBeDisabled();
  });

  it('publishes dataset and shows success feedback (WKS2-23)', async () => {
    renderPublish('/workspace/templates/publish?datasetId=5');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar publicação' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar publicação' }));
    await waitFor(() => expect(publishDatasetTemplate).toHaveBeenCalledWith(5));
    expect(screen.getByText(/Template "Vendas" publicado/i)).toBeInTheDocument();
  });

  it('publishes widget successfully', async () => {
    renderPublish('/workspace/templates/publish?widgetId=7');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar publicação' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar publicação' }));
    await waitFor(() => expect(publishWidgetTemplate).toHaveBeenCalledWith(7));
    expect(screen.getByText(/Template "KPI Vendas" publicado/i)).toBeInTheDocument();
  });

  it('shows idempotent message when hash unchanged (WKS2-23)', async () => {
    vi.mocked(publishDatasetTemplate).mockResolvedValue({
      id: 1,
      nome: 'Vendas',
      tipo: 'DATASET',
      versaoAtual: 1,
      estruturaHash: 'abc',
      novaVersaoCriada: false,
    });
    renderPublish('/workspace/templates/publish?datasetId=5');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar publicação' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar publicação' }));
    await waitFor(() =>
      expect(screen.getByText(/já estava publicado com a mesma estrutura/i)).toBeInTheDocument(),
    );
  });
});
