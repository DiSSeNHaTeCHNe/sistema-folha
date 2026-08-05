import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { CompetenciaSelector } from './CompetenciaSelector';
import { renderWithProviders } from '../../test/renderWithProviders';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';
import { limparCompetenciaGlobal } from './competenciaStorage';
import { useCompetenciaGlobal } from './hooks/useCompetenciaGlobal';
import { renderHook, act } from '@testing-library/react';

vi.mock('../../services/resumoFolhaPagamentoService', () => ({
  resumoFolhaPagamentoService: {
    listarMaisRecentes: vi.fn(),
  },
}));

const mockResumos = [
  {
    id: 1,
    competenciaInicio: '2026-06-01',
    competenciaFim: '2026-06-30',
    totalEmpregados: 10,
    totalEncargos: 0,
    totalPagamentos: 0,
    totalDescontos: 0,
    totalLiquido: 0,
    totalBruto: 0,
    totalCustoEmpresa: 0,
    dataImportacao: '2026-06-01',
    decimoTerceiro: false,
    ativo: true,
  },
  {
    id: 2,
    competenciaInicio: '2026-05-01',
    competenciaFim: '2026-05-31',
    totalEmpregados: 9,
    totalEncargos: 0,
    totalPagamentos: 0,
    totalDescontos: 0,
    totalLiquido: 0,
    totalBruto: 0,
    totalCustoEmpresa: 0,
    dataImportacao: '2026-05-01',
    decimoTerceiro: false,
    ativo: true,
  },
];

describe('CompetenciaSelector', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    limparCompetenciaGlobal();
    vi.mocked(resumoFolhaPagamentoService.listarMaisRecentes).mockResolvedValue(mockResumos);
  });

  it('defaults to mais recente (null competencia)', async () => {
    const onChange = vi.fn();
    renderWithProviders(<CompetenciaSelector value={null} onChange={onChange} />);
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Competência' })).toBeInTheDocument());
    expect(screen.getByRole('combobox', { name: 'Competência' })).toHaveTextContent('Mais recente');
  });

  it('refetches widgets when competencia changes (DASHC-28)', async () => {
    const onChange = vi.fn();
    renderWithProviders(<CompetenciaSelector value={null} onChange={onChange} />);
    await waitFor(() => expect(resumoFolhaPagamentoService.listarMaisRecentes).toHaveBeenCalled());

    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Competência' }));
    const listbox = await screen.findByRole('listbox');
    fireEvent.click(within(listbox).getByRole('option', { name: 'Mai/2026' }));
    expect(onChange).toHaveBeenCalledWith('2026-05');
  });

  it('lists available competencias from resumo folha', async () => {
    renderWithProviders(<CompetenciaSelector value={null} onChange={() => {}} />);
    await waitFor(() => expect(resumoFolhaPagamentoService.listarMaisRecentes).toHaveBeenCalled());
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Competência' }));
    const listbox = await screen.findByRole('listbox');
    expect(within(listbox).getByRole('option', { name: 'Jun/2026' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Mai/2026' })).toBeInTheDocument();
  });
});

describe('useCompetenciaGlobal', () => {
  beforeEach(() => {
    limparCompetenciaGlobal();
  });

  it('persists competencia in sessionStorage (DASHC-31)', () => {
    const { result } = renderHook(() => useCompetenciaGlobal());
    expect(result.current.competenciaGlobal).toBeNull();

    act(() => {
      result.current.setCompetenciaGlobal('2026-03');
    });
    expect(result.current.competenciaGlobal).toBe('2026-03');

    const { result: restored } = renderHook(() => useCompetenciaGlobal());
    expect(restored.current.competenciaGlobal).toBe('2026-03');
  });

  it('clears to null for mais recente default', () => {
    const { result } = renderHook(() => useCompetenciaGlobal());
    act(() => {
      result.current.setCompetenciaGlobal('2026-03');
    });
    act(() => {
      result.current.setCompetenciaGlobal(null);
    });
    expect(result.current.competenciaGlobal).toBeNull();
  });
});
