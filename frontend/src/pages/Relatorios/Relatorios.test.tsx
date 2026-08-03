import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { Relatorios } from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { relatorioService } from '../../services/relatorioService';

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

vi.mock('../../services/relatorioService', () => ({
  relatorioService: {
    listarRelatoriosFolha: vi.fn(),
    listarRelatoriosBeneficio: vi.fn(),
    gerarRelatorioFolha: vi.fn(),
    gerarRelatorioBeneficio: vi.fn(),
    downloadRelatorioFolha: vi.fn(),
    downloadRelatorioBeneficio: vi.fn(),
  },
}));

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
    setupMocks();
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');
    vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });

  it('renders the page title without real HTTP', async () => {
    renderWithProviders(<Relatorios />);
    expect(screen.getByRole('heading', { name: 'Relatórios' })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Folha de Pagamento' })).toBeInTheDocument());
  });

  it('shows load error notification', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao carregar relatórios', 'error'));
  });

  it('lists folha reports and downloads processed report', async () => {
    vi.mocked(relatorioService.downloadRelatorioFolha).mockResolvedValue(new Blob(['pdf']));
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByText('6/2026')).toBeInTheDocument());

    const row = screen.getByText('6/2026').closest('tr')!;
    fireEvent.click(within(row).getByRole('button'));

    await waitFor(() => {
      expect(relatorioService.downloadRelatorioFolha).toHaveBeenCalledWith(1);
      expect(showNotification).toHaveBeenCalledWith('Relatório baixado com sucesso', 'success');
    });
  });

  it('disables download for non-processed folha report', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPending]);
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByText('6/2026')).toBeInTheDocument());
    const row = screen.getByText('6/2026').closest('tr')!;
    expect(within(row).getByRole('button')).toBeDisabled();
  });

  it('generates folha report successfully', async () => {
    vi.mocked(relatorioService.gerarRelatorioFolha).mockResolvedValue(folhaReport);
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Gerar Relatório' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await waitFor(() => {
      expect(relatorioService.gerarRelatorioFolha).toHaveBeenCalled();
      expect(showNotification).toHaveBeenCalledWith('Relatório de folha gerado com sucesso', 'success');
    });
  });

  it('shows generate error on folha tab', async () => {
    vi.mocked(relatorioService.gerarRelatorioFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Gerar Relatório' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao gerar relatório', 'error'));
  });

  it('shows download error on folha tab', async () => {
    vi.mocked(relatorioService.downloadRelatorioFolha).mockRejectedValue(new Error('fail'));
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByText('6/2026')).toBeInTheDocument());
    fireEvent.click(within(screen.getByText('6/2026').closest('tr')!).getByRole('button'));
    await waitFor(() => expect(showNotification).toHaveBeenCalledWith('Erro ao baixar relatório', 'error'));
  });

  it('switches to beneficio tab and generates report', async () => {
    vi.mocked(relatorioService.gerarRelatorioBeneficio).mockResolvedValue(beneficioReport);
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Benefícios' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Benefícios' }));
    await waitFor(() => expect(screen.getByText('6/2026')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await waitFor(() => {
      expect(relatorioService.gerarRelatorioBeneficio).toHaveBeenCalled();
      expect(showNotification).toHaveBeenCalledWith('Relatório de benefícios gerado com sucesso', 'success');
    });
  });

  it('downloads beneficio report from beneficio tab', async () => {
    vi.mocked(relatorioService.downloadRelatorioBeneficio).mockResolvedValue(new Blob(['pdf']));
    renderWithProviders(<Relatorios />);
    fireEvent.click(await screen.findByRole('tab', { name: 'Benefícios' }));
    await waitFor(() => expect(screen.getByText('6/2026')).toBeInTheDocument());
    fireEvent.click(within(screen.getByText('6/2026').closest('tr')!).getByRole('button'));
    await waitFor(() => {
      expect(relatorioService.downloadRelatorioBeneficio).toHaveBeenCalledWith(3);
      expect(showNotification).toHaveBeenCalledWith('Relatório baixado com sucesso', 'success');
    });
  });

  it('shows dash when dataProcessamento is missing', async () => {
    vi.mocked(relatorioService.listarRelatoriosFolha).mockResolvedValue([folhaPending]);
    renderWithProviders(<Relatorios />);
    await waitFor(() => expect(screen.getByText('-')).toBeInTheDocument());
  });
});
