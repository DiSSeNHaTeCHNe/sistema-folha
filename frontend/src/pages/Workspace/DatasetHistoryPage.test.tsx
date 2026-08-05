import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import DatasetHistoryPage from './DatasetHistoryPage';
import { renderWithProviders } from '../../test/renderWithProviders';
import { getDataset, listDatasetAudit, listDatasetRowAudit } from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  getDataset: vi.fn(),
  listDatasetAudit: vi.fn(),
  listDatasetRowAudit: vi.fn(),
}));

const timeline = [
  {
    rowId: 100,
    acao: 'UPDATE' as const,
    autorUsuarioId: 7,
    dataEvento: '2026-01-15T10:30:00Z',
    resumo: 'valor alterado',
  },
  {
    rowId: 100,
    acao: 'CREATE' as const,
    autorUsuarioId: 7,
    dataEvento: '2026-01-10T08:00:00Z',
    resumo: null,
  },
  {
    rowId: 101,
    acao: 'CREATE' as const,
    autorUsuarioId: 8,
    dataEvento: '2026-01-12T09:00:00Z',
    resumo: null,
  },
];

const rowAudit = [
  {
    id: 1,
    rowId: 100,
    autorUsuarioId: 7,
    acao: 'CREATE' as const,
    valoresAnteriores: null,
    valoresNovos: { valor: '10' },
    dataEvento: '2026-01-10T08:00:00Z',
  },
  {
    id: 2,
    rowId: 100,
    autorUsuarioId: 7,
    acao: 'UPDATE' as const,
    valoresAnteriores: { valor: '10' },
    valoresNovos: { valor: '20' },
    dataEvento: '2026-01-15T10:30:00Z',
  },
];

function renderHistory(route = '/workspace/datasets/5/historico') {
  return renderWithProviders(
    <Routes>
      <Route path="/workspace/datasets/:id/historico" element={<DatasetHistoryPage />} />
    </Routes>,
    { route },
  );
}

describe('DatasetHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getDataset).mockResolvedValue({
      id: 5,
      nome: 'Vendas',
      campos: [{ nome: 'valor', tipo: 'MOEDA' }],
      schemaVersion: 1,
      totalLinhas: 2,
    });
    vi.mocked(listDatasetAudit).mockResolvedValue(timeline);
    vi.mocked(listDatasetRowAudit).mockResolvedValue(rowAudit);
  });

  it('renders chronological timeline with author and timestamp (WKS2-28)', async () => {
    renderHistory();
    await waitFor(() => expect(screen.getByRole('heading', { name: /Histórico — Vendas/i, level: 1 })).toBeInTheDocument());
    expect(screen.getByRole('list', { name: 'Timeline de auditoria' })).toBeInTheDocument();
    expect(screen.getByText(/Linha 100 — Edição/i)).toBeInTheDocument();
    expect(screen.getAllByText(/autor #7/i).length).toBeGreaterThan(0);
  });

  it('loads row drill-down when timeline entry selected (WKS2-29)', async () => {
    renderHistory();
    await waitFor(() => expect(screen.getAllByLabelText('Ver histórico da linha 100').length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByLabelText('Ver histórico da linha 100')[0]);
    await waitFor(() => expect(listDatasetRowAudit).toHaveBeenCalledWith(5, 100));
    expect(screen.getByRole('list', { name: 'Histórico da linha 100' })).toBeInTheDocument();
    expect(screen.getAllByText(/Autor #7/i).length).toBeGreaterThan(0);
  });

  it('shows explanatory empty state when no audit entries (WKS2-30)', async () => {
    vi.mocked(listDatasetAudit).mockResolvedValue([]);
    renderHistory();
    await waitFor(() =>
      expect(screen.getByText(/Nenhuma alteração registrada/i)).toBeInTheDocument(),
    );
    expect(screen.queryByRole('list', { name: 'Timeline de auditoria' })).not.toBeInTheDocument();
  });

  it('shows invalid id message', () => {
    renderHistory('/workspace/datasets/abc/historico');
    expect(screen.getByText('Identificador de dataset inválido.')).toBeInTheDocument();
  });

  it('shows error banner when load fails', async () => {
    vi.mocked(getDataset).mockRejectedValue(new Error('fail'));
    renderHistory();
    await waitFor(() => expect(screen.getByText(/Erro ao carregar histórico/i)).toBeInTheDocument());
  });

  it('links back to dataset editor', async () => {
    renderHistory();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Voltar ao editor' })).toBeInTheDocument());
  });
});
