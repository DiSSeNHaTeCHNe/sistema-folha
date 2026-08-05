import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import TemplateUpgradePage from './TemplateUpgradePage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { listTemplateVersions, upgradeTemplateInstallation } from '../../services/workspaceService';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../services/workspaceService', () => ({
  listTemplateVersions: vi.fn(),
  upgradeTemplateInstallation: vi.fn(),
  WorkspaceApiError: class WorkspaceApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
}));

const versions = [
  {
    versao: 4,
    dataPublicacao: '2026-01-02',
    estruturaResumo: {
      campos: ['valor', 'centro'],
      widgets: ['KPI Total'],
      formulas: ['SOMA(valor)'],
    },
  },
  {
    versao: 3,
    dataPublicacao: '2025-12-01',
    estruturaResumo: {
      campos: ['valor'],
      widgets: ['KPI Total'],
      formulas: ['SOMA(valor)'],
    },
  },
];

function renderUpgrade(route = '/workspace/templates/2/upgrade?installationId=10&versaoInstalada=3') {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/templates/:templateId/upgrade" element={<TemplateUpgradePage />} />
    </Routes>,
    { route },
  );
}

describe('TemplateUpgradePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockReset();
    vi.mocked(listTemplateVersions).mockResolvedValue(versions);
    vi.mocked(upgradeTemplateInstallation).mockResolvedValue({
      installationId: 10,
      templateId: 2,
      versaoInstalada: 4,
      workspaceId: 1,
      datasetId: null,
      widgetDefinitionIds: [7],
    });
  });

  it('lists structural diff between installed and latest versions (WKS2-26)', async () => {
    renderUpgrade();
    await waitFor(() => expect(screen.getByText('Comparando v3 → v4')).toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Campos adicionados', level: 3 })).toBeInTheDocument();
    expect(screen.getByText('centro')).toBeInTheDocument();
  });

  it('shows version chips for installed and available (WKS2-25)', async () => {
    renderUpgrade();
    await waitFor(() => expect(screen.getByText('Instalada: v3')).toBeInTheDocument());
    expect(screen.getByText('Disponível: v4')).toBeInTheDocument();
  });

  it('upgrades when user chooses Atualizar (WKS2-27)', async () => {
    renderUpgrade();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Atualizar' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }));
    await waitFor(() => expect(upgradeTemplateInstallation).toHaveBeenCalledWith(10));
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/templates');
  });

  it('returns to catalog without upgrade when Permanecer is chosen (WKS2-27)', async () => {
    renderUpgrade();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Permanecer na versão atual' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Permanecer na versão atual' }));
    expect(upgradeTemplateInstallation).not.toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/templates');
  });

  it('shows error for invalid params', async () => {
    renderUpgrade('/workspace/templates/x/upgrade?installationId=10&versaoInstalada=3');
    await waitFor(() => expect(screen.getByText(/Parâmetros inválidos/i)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Atualizar' })).toBeDisabled();
  });

  it('shows load error when versions fail', async () => {
    vi.mocked(listTemplateVersions).mockRejectedValue(new Error('fail'));
    renderUpgrade();
    await waitFor(() => expect(screen.getByText(/Erro ao carregar versões/i)).toBeInTheDocument());
  });
});
