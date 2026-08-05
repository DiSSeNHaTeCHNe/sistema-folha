import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import TemplateCatalogPage from './TemplateCatalogPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  installOrcamentoTemplate,
  installTemplate,
  listTemplateCatalog,
  listWorkspaces,
  upgradeTemplateInstallation,
} from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  listTemplateCatalog: vi.fn(),
  listWorkspaces: vi.fn(),
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
    versaoMaisRecente: 4,
    atualizacaoDisponivel: true,
    publicadorUsuarioId: 6,
    installationId: 10,
    versaoInstalada: 3,
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

  it('renders heading and catalog cards for scoped user (WKS2-21)', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Catálogo de Templates', level: 1 })).toBeInTheDocument(),
    );
    expect(screen.getByRole('heading', { name: 'Orçamento CC' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'KPI Folha' })).toBeInTheDocument();
  });

  it('distinguishes native vs user published templates (WKS2-21)', async () => {
    const nativeCatalog = [
      {
        id: 0,
        nome: 'Orçamento por CC',
        tipo: 'PACOTE' as const,
        versaoAtual: 1,
        versaoMaisRecente: 1,
        atualizacaoDisponivel: false,
        publicadorUsuarioId: 0,
        installationId: null,
        versaoInstalada: null,
      },
      ...sampleCatalog,
    ];
    vi.mocked(listTemplateCatalog).mockResolvedValue(nativeCatalog);
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByText('Nativo')).toBeInTheDocument());
    expect(screen.getAllByText('Usuário').length).toBeGreaterThanOrEqual(1);
  });

  it('shows version chip on catalog cards (WKS2-21)', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByText('v2')).toBeInTheDocument());
    expect(screen.getByText('v3')).toBeInTheDocument();
  });

  it('shows vN disponível indicator when upgrade available (WKS2-25)', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByText('v4 disponível')).toBeInTheDocument());
  });

  it('shows Ver diferenças link when upgrade available (WKS2-25)', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('link', { name: 'Ver diferenças de KPI Folha' })).toHaveAttribute(
        'href',
        '/workspace/templates/2/upgrade?installationId=10&versaoInstalada=3',
      ),
    );
  });

  it('links publish button to dedicated publish page (WKS2-22)', async () => {
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() => expect(screen.getByRole('link', { name: 'Publicar template' })).toBeInTheDocument());
    expect(screen.getByRole('link', { name: 'Publicar template' })).toHaveAttribute(
      'href',
      '/workspace/templates/publish',
    );
  });

  it('shows empty state when catalog is empty', async () => {
    vi.mocked(listTemplateCatalog).mockResolvedValue([]);
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByText(/Nenhum template visível/i)).toBeInTheDocument(),
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

  it('installs native orcamento template via orcamento endpoint', async () => {
    const nativeCatalog = [
      {
        id: 0,
        nome: 'Orçamento por CC',
        tipo: 'PACOTE' as const,
        versaoAtual: 1,
        versaoMaisRecente: 1,
        atualizacaoDisponivel: false,
        publicadorUsuarioId: 0,
        installationId: null,
        versaoInstalada: null,
      },
    ];
    vi.mocked(listTemplateCatalog).mockResolvedValue(nativeCatalog);
    vi.mocked(installOrcamentoTemplate).mockResolvedValue({
      workspaceId: 1,
      datasetId: 50,
      widgetDefinitionIds: [60, 61],
      widgetsAdicionados: 2,
    });
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Instalar template Orçamento por CC' })).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Instalar template Orçamento por CC' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar instalação' }));
    await waitFor(() => expect(installOrcamentoTemplate).toHaveBeenCalledWith(1));
    expect(installTemplate).not.toHaveBeenCalled();
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
      installationId: 10, templateId: 2, versaoInstalada: 4, workspaceId: 1, datasetId: null, widgetDefinitionIds: [7],
    });
    renderWithProviders(<TemplateCatalogPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Atualizar template KPI Folha para versão 4' })).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar template KPI Folha para versão 4' }));
    await waitFor(() => expect(upgradeTemplateInstallation).toHaveBeenCalledWith(10));
  });
});
