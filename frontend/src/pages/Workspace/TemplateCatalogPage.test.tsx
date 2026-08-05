import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import TemplateCatalogPage from './TemplateCatalogPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  installTemplate,
  listDatasets,
  listTemplateCatalog,
  listWidgetDefinitions,
  listWorkspaces,
  publishDatasetTemplate,
  upgradeTemplateInstallation,
} from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  listTemplateCatalog: vi.fn(),
  listWorkspaces: vi.fn(),
  listDatasets: vi.fn(),
  listWidgetDefinitions: vi.fn(),
  publishDatasetTemplate: vi.fn(),
  publishWidgetTemplate: vi.fn(),
  installTemplate: vi.fn(),
  upgradeTemplateInstallation: vi.fn(),
  installOrcamentoTemplate: vi.fn(),
}));

const sampleCatalog = [
  {
    id: 1,
    nome: 'Orçamento CC',
    tipo: 'DATASET' as const,
    versaoAtual: 2,
    versaoMaisRecente: 2,
    atualizacaoDisponivel: false,
    publicadorUsuarioId: 5,
    installationId: null,
    versaoInstalada: null,
  },
  {
    id: 2,
    nome: 'KPI Folha',
    tipo: 'WIDGET' as const,
    versaoAtual: 3,
    versaoMaisRecente: 3,
    atualizacaoDisponivel: true,
    publicadorUsuarioId: 6,
    installationId: 10,
    versaoInstalada: 1,
  },
];

describe('TemplateCatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listTemplateCatalog).mockResolvedValue(sampleCatalog);
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Principal', totalWidgets: 0 }]);
  });

  it('shows loading spinner initially', () => {
    vi.mocked(listTemplateCatalog).mockReturnValue(new Promise(() => {}));
    renderWithProviders(<TemplateCatalogPage />);
    expect(screen.getByLabelText('Carregando catálogo de templates')).toBeInTheDocument();
  });

  it('renders heading and catalog cards for scoped user', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Catálogo de Templates' })).toBeInTheDocument(),
    );
    expect(screen.getByRole('heading', { name: 'Orçamento CC' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'KPI Folha' })).toBeInTheDocument();
  });

  it('shows empty state when catalog is empty', async () => {
    vi.mocked(listTemplateCatalog).mockResolvedValue([]);
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(/Nenhum template visível/i),
    );
  });

  it('shows install button for not-yet-installed template', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Instalar template Orçamento CC' })).toBeInTheDocument(),
    );
  });

  it('shows installed state for already installed template', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Template KPI Folha já instalado' })).toBeDisabled(),
    );
  });

  it('opens publish dialog with explicit opt-in flow', async () => {
    vi.mocked(listDatasets).mockResolvedValue([{ id: 5, nome: 'Vendas', schemaVersion: 1, totalLinhas: 0, totalCampos: 1 }]);
    vi.mocked(listWidgetDefinitions).mockResolvedValue([]);
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Publicar template' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Publicar template' }));
    expect(screen.getByRole('dialog', { name: 'Publicar template' })).toBeInTheDocument();
    expect(screen.getByText(/Apenas a estrutura é publicada/i)).toBeInTheDocument();
  });

  it('requires saved item selection before publish', async () => {
    vi.mocked(listDatasets).mockResolvedValue([]);
    vi.mocked(listWidgetDefinitions).mockResolvedValue([]);
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Publicar template' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Publicar template' }));
    const dialog = screen.getByRole('dialog', { name: 'Publicar template' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Publicar' }));
    await waitFor(() =>
      expect(screen.getByText('Selecione um dataset ou widget salvo para publicar')).toBeInTheDocument(),
    );
    expect(publishDatasetTemplate).not.toHaveBeenCalled();
  });

  it('installs template into selected workspace', async () => {
    vi.mocked(installTemplate).mockResolvedValue({
      installationId: 20, templateId: 1, versaoInstalada: 2, workspaceId: 1, datasetId: 50, widgetDefinitionIds: [],
    });
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Instalar template Orçamento CC' })).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Instalar template Orçamento CC' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar instalação' }));
    await waitFor(() => expect(installTemplate).toHaveBeenCalledWith(1, 1));
  });
});

describe('TemplateCatalogPage upgrade banner', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listTemplateCatalog).mockResolvedValue(sampleCatalog);
    vi.mocked(listWorkspaces).mockResolvedValue([{ id: 1, nome: 'Principal', totalWidgets: 0 }]);
  });

  it('shows upgrade banner when update available', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('status', { name: /Atualização disponível para KPI Folha/i })).toBeInTheDocument(),
    );
  });

  it('triggers optional upgrade without forcing user', async () => {
    vi.mocked(upgradeTemplateInstallation).mockResolvedValue({
      installationId: 10, templateId: 2, versaoInstalada: 3, workspaceId: 1, datasetId: null, widgetDefinitionIds: [7],
    });
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Atualizar template KPI Folha para versão 3' })).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar template KPI Folha para versão 3' }));
    await waitFor(() => expect(upgradeTemplateInstallation).toHaveBeenCalledWith(10));
  });
});
