import { beforeEach, describe, expect, it, vi, afterEach } from 'vitest';
import { fireEvent, screen, waitFor, act } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import { relatorioService, RELATORIO_GERACAO_TIMEOUT_MS, resolveRelatorioApiError } from '../../services/relatorioService';

vi.mock('./CompetenciaPicker', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./CompetenciaPicker')>();
  return {
    ...actual,
    CompetenciaPicker: ({
      value,
      onChange,
    }: {
      value: { mes: number; ano: number };
      onChange: (competencia: { mes: number; ano: number }) => void;
    }) => (
      <div>
        <span aria-label="Selecionar competência mês e ano">
          {value.mes}/{value.ano}
        </span>
        <button
          type="button"
          aria-label="Definir competência março de 2025"
          onClick={() => onChange({ mes: 3, ano: 2025 })}
        >
          Mar/2025
        </button>
      </div>
    ),
  };
});

import { Relatorios } from './index';

const showNotification = vi.fn();
const hideNotification = vi.fn();

const folhaReport = {
  id: 1,
  mes: 6,
  ano: 2026,
  totalFuncionarios: 10,
  totalFolha: 50000,
  totalBeneficios: 2000,
  status: 'PROCESSADO' as const,
  dataProcessamento: '2026-06-15T10:00:00',
};

const folhaPending = {
  ...folhaReport,
  id: 2,
  status: 'PENDENTE' as const,
  dataProcessamento: undefined,
  stale: false,
};

const folhaPendingStale = {
  ...folhaPending,
  id: 5,
  stale: true,
};

const folhaError = {
  ...folhaReport,
  id: 4,
  status: 'ERRO' as const,
  erro: 'Falha interna na geração',
  dataProcessamento: undefined,
};

const beneficioReport = {
  id: 3,
  mes: 6,
  ano: 2026,
  totalBeneficios: 5,
  totalValor: 1500,
  status: 'PROCESSADO' as const,
  dataProcessamento: '2026-06-15T10:00:00',
};

vi.mock('../../services/relatorioService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/relatorioService')>();
  return {
    ...actual,
    relatorioService: {
      listarRelatoriosFolha: vi.fn(),
      listarRelatoriosBeneficio: vi.fn(),
      gerarRelatorioFolha: vi.fn(),
      gerarRelatorioBeneficio: vi.fn(),
      downloadRelatorioFolha: vi.fn(),
      downloadRelatorioBeneficio: vi.fn(),
    },
  };
});

vi.mock('../../hooks/useNotification', () => ({
  useNotification: () => ({
    notification: { open: false, message: '', severity: 'info' },
    showNotification,
    hideNotification,
  }),
}));

function setupMocks() {
  vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaReport]);
  vi.mocked(relatorioService.listarRelatoriosBeneficio).mockResolvedValue([beneficioReport]);
}

describe('Relatorios page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.setSystemTime(new Date('2026-06-15T12:00:00'));
    vi.useFakeTimers({ shouldAdvanceTime: true });
    setupMocks();
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');
    vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.setSystemTime(new Date());
  });

  it('renders hub with catalog cards and competencia picker', async () => {
    renderWithProviders(<Relatorios />);
    expect(await screen.findByRole('heading', { name: 'Relatórios Executivos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Executivo de Folha' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Custo Benefício + Folha' })).toBeInTheDocument();
    expect(screen.getByLabelText('Selecionar competência mês e ano')).toBeInTheDocument();
  });

  it('shows load error notification', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao carregar relatórios', 'error'));
  });

  it('generates folha report using selected competencia from picker', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockResolvedValue({
      ...folhaReport,
      mes: 3,
      ano: 2025,
    });
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar Executivo de Folha' });

    const picker = screen.getByLabelText('Selecionar competência mês e ano');
    expect(picker).toHaveTextContent('6/2026');
    fireEvent.click(screen.getByRole('button', { name: 'Definir competência março de 2025' }));
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Executivo de Folha' }));

    await waitFor(() => {
      expect(relatorioService.gerarRelatorioFolha).toHaveBeenCalledWith(3, 2025);
      expect(showNotification).toHaveBeenCalledWith('Relatório de folha em geração', 'success');
    });
  });

  it('generates folha report using default competencia when list has match', async () => {
    vi.mocked(relatorioService.gerarRelatorioFolha).mockResolvedValue(folhaReport);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('heading', { name: 'Executivo de Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Gerar novamente Executivo de Folha' }));

    await waitFor(() => {
      expect(relatorioService.gerarRelatorioFolha).toHaveBeenCalledWith(6, 2026);
      expect(showNotification).toHaveBeenCalledWith('Relatório de folha em geração', 'success');
    });
  });

  it('downloads processed folha report', async () => {
    vi.mocked(relatorioService.downloadRelatorioFolha).mockResolvedValue(new Blob(['pdf']));
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Baixar PDF Executivo de Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Baixar PDF Executivo de Folha' }));

    await waitFor(() => {
      expect(relatorioService.downloadRelatorioFolha).toHaveBeenCalledWith(1);
      expect(showNotification).toHaveBeenCalledWith('Relatório baixado com sucesso', 'success');
    });
  });

  it('disables generation while folha report is pending', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPending]);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('status');
    expect(screen.getByRole('button', { name: /aguardando processamento/i })).toBeDisabled();
  });

  it('polls list while folha report is pending', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPending]);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('status');

    const callsBefore = vi.mocked(relatorioService.listarRelatoriosFolha).mock.calls.length;

    await act(async () => {
      vi.advanceTimersByTime(2100);
    });

    await waitFor(() => {
      expect(vi.mocked(relatorioService.listarRelatoriosFolha).mock.calls.length).toBeGreaterThan(callsBefore);
    });
  });

  it('shows error state with retry for folha report', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaError]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockResolvedValue(folhaReport);
    renderWithProviders(<Relatorios />);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent('Falha interna na geração');

    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente Executivo de Folha' }));

    await waitFor(() => {
      expect(relatorioService.gerarRelatorioFolha).toHaveBeenCalled();
    });
  });

  it('generates beneficio report successfully', async () => {
    vi.mocked(relatorioService.gerarRelatorioBeneficio).mockResolvedValue(beneficioReport);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('heading', { name: 'Custo Benefício + Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Gerar novamente Custo Benefício + Folha' }));

    await waitFor(() => {
      expect(relatorioService.gerarRelatorioBeneficio).toHaveBeenCalled();
      expect(showNotification).toHaveBeenCalledWith('Relatório de benefícios em geração', 'success');
    });
  });

  it('downloads beneficio report when processed', async () => {
    vi.mocked(relatorioService.downloadRelatorioBeneficio).mockResolvedValue(new Blob(['pdf']));
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Baixar PDF Custo Benefício + Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Baixar PDF Custo Benefício + Folha' }));

    await waitFor(() => {
      expect(relatorioService.downloadRelatorioBeneficio).toHaveBeenCalledWith(3);
      expect(showNotification).toHaveBeenCalledWith('Relatório baixado com sucesso', 'success');
    });
  });

  it('shows generate error on folha card', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar Executivo de Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Gerar Executivo de Folha' }));

    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao gerar relatório', 'error'));
  });

  it('shows 429 limit message on generate failure', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockRejectedValue({
      isAxiosError: true,
      response: { status: 429, data: { message: 'Limite de 3 gerações simultâneas por usuário atingido' } },
    });
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar Executivo de Folha' });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Executivo de Folha' }));
    await waitFor(() =>
      expect(showNotification).toHaveBeenCalledWith(
        'Limite de 3 gerações simultâneas por usuário atingido',
        'error',
      ),
    );
  });

  it('shows 403 access denied message on generate failure', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockRejectedValue({
      isAxiosError: true,
      response: { status: 403, data: { message: 'Acesso negado' } },
    });
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar Executivo de Folha' });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Executivo de Folha' }));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Acesso negado', 'error'));
  });

  it('shows timeout message on client abort', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockRejectedValue({
      isAxiosError: true,
      code: 'ECONNABORTED',
      message: 'timeout of 65000ms exceeded',
    });
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar Executivo de Folha' });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Executivo de Folha' }));
    await waitFor(() =>
      expect(showNotification).toHaveBeenCalledWith(
        expect.stringContaining('Tempo esgotado na requisição'),
        'error',
      ),
    );
  });

  it('uses POST timeout of at least 65000ms', () => {
    expect(RELATORIO_GERACAO_TIMEOUT_MS).toBeGreaterThanOrEqual(65_000);
  });

  it('shows retry button for stale pending folha report', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPendingStale]);
    vi.mocked(relatorioService.gerarRelatorioFolha).mockResolvedValue(folhaReport);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Tentar novamente Executivo de Folha' });
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente Executivo de Folha' }));
    await waitFor(() => expect(relatorioService.gerarRelatorioFolha).toHaveBeenCalled());
  });

  it('does not poll when folha report is stale pending', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPendingStale]);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Tentar novamente Executivo de Folha' });
    const callsBefore = vi.mocked(relatorioService.listarRelatoriosFolha).mock.calls.length;
    await act(async () => {
      vi.advanceTimersByTime(2100);
    });
    expect(vi.mocked(relatorioService.listarRelatoriosFolha).mock.calls.length).toBe(callsBefore);
  });

  it('shows generate button when user has no report for selected competencia', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaReport]);
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Gerar novamente Executivo de Folha' });
    fireEvent.click(screen.getByRole('button', { name: 'Definir competência março de 2025' }));
    expect(await screen.findByRole('button', { name: 'Gerar Executivo de Folha' })).toBeInTheDocument();
  });

  it('resolveRelatorioApiError maps axios statuses', () => {
    expect(
      resolveRelatorioApiError({
        isAxiosError: true,
        response: { status: 429, data: { message: 'Limite atingido' } },
      }),
    ).toBe('Limite atingido');
  });

  it('shows download error on folha card', async () => {
    vi.mocked(relatorioService.downloadRelatorioFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await screen.findByRole('button', { name: 'Baixar PDF Executivo de Folha' });

    fireEvent.click(screen.getByRole('button', { name: 'Baixar PDF Executivo de Folha' }));

    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao baixar relatório', 'error'));
  });
});
