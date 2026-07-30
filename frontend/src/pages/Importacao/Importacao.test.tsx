import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { toast } from 'react-toastify';
import Importacao from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';

vi.mock('../../services/importacaoService', () => ({
  importacaoService: {
    importarFolhaAdp: vi.fn(),
    importarBeneficios: vi.fn(),
  },
}));

vi.mock('../../services/beneficioMensalService', () => ({
  beneficioMensalService: {
    listarCompetencias: vi.fn().mockResolvedValue([]),
    importar: vi.fn(),
  },
}));

vi.mock('../../services/folhaPagamentoService', () => ({
  folhaPagamentoService: {
    listarCompetencias: vi.fn().mockResolvedValue([]),
    processarCompetencia: vi.fn(),
  },
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

describe('Importacao page', () => {
  beforeEach(() => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockReset();
  });

  it('renders the page title without real HTTP', () => {
    renderWithProviders(<Importacao />);

    expect(screen.getByRole('heading', { name: 'Importação de Dados' })).toBeInTheDocument();
  });

  it('shows beneficios mensais upload section', () => {
    renderWithProviders(<Importacao />);

    expect(screen.getByText('Importação de Benefícios Mensais')).toBeInTheDocument();
    expect(screen.getByText('Importação de Folha ADP')).toBeInTheDocument();
  });

  it('shows processamento manual section', () => {
    renderWithProviders(<Importacao />);

    expect(screen.getByText('Processar ficha da competência')).toBeInTheDocument();
  });

  it('opens help dialog', async () => {
    renderWithProviders(<Importacao />);

    fireEvent.click(screen.getByTestId('HelpOutlineIcon').closest('button')!);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByText('Ajuda - Importação de Dados')).toBeInTheDocument();
  });

  it('closes help dialog', async () => {
    renderWithProviders(<Importacao />);

    fireEvent.click(screen.getByTestId('HelpOutlineIcon').closest('button')!);
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('rejects non-txt file for folha ADP', async () => {
    renderWithProviders(<Importacao />);

    const inputs = document.querySelectorAll('input[type="file"]');
    const folhaInput = inputs[1] as HTMLInputElement;
    const file = new File(['data'], 'folha.csv', { type: 'text/csv' });
    fireEvent.change(folhaInput, { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    expect(toast.error).toHaveBeenCalledWith(
      'Para importação de folha ADP, selecione apenas arquivos .txt',
    );
  });

  it('processes manual competencia when Processar is clicked', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockResolvedValue({
      totalFichas: 1,
      totalLinhas: 5,
      totalFuncionarios: 1,
    });

    renderWithProviders(<Importacao />);

    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(folhaPagamentoService.processarCompetencia).toHaveBeenCalled();
    });
    expect(toast.success).toHaveBeenCalled();
  });

  it('toggles decimo terceiro checkbox for folha ADP', () => {
    renderWithProviders(<Importacao />);

    const checkboxes = screen.getAllByRole('checkbox', { name: /13º salário/i });
    const folhaCheckbox = checkboxes[0];
    expect(folhaCheckbox).not.toBeChecked();

    fireEvent.click(folhaCheckbox);
    expect(folhaCheckbox).toBeChecked();
  });
});
