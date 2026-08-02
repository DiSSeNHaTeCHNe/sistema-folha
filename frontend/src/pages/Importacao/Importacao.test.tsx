import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { toast } from 'react-toastify';
import Importacao from './index';
import { renderWithProviders } from '../../test/renderWithProviders';
import { importacaoService } from '../../services/importacaoService';
import { beneficioMensalService } from '../../services/beneficioMensalService';
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

function getFileInputs() {
  return document.querySelectorAll('input[type="file"]');
}

function selectBeneficiosFile(name = 'beneficios.xlsx') {
  const input = getFileInputs()[0] as HTMLInputElement;
  const file = new File(['data'], name, {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  fireEvent.change(input, { target: { files: [file] } });
  return file;
}

function selectFolhaFile(name = 'folha.txt') {
  const input = getFileInputs()[1] as HTMLInputElement;
  const file = new File(['data'], name, { type: 'text/plain' });
  fireEvent.change(input, { target: { files: [file] } });
  return file;
}

describe('Importacao page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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

  it('opens and closes help dialog', async () => {
    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByTestId('HelpOutlineIcon').closest('button')!);
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    expect(screen.getByText('Ajuda - Importação de Dados')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('rejects non-txt file for folha ADP', async () => {
    renderWithProviders(<Importacao />);
    const folhaInput = getFileInputs()[1] as HTMLInputElement;
    fireEvent.change(folhaInput, { target: { files: [new File(['data'], 'folha.csv', { type: 'text/csv' })] } });
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));
    expect(toast.error).toHaveBeenCalledWith(
      'Para importação de folha ADP, selecione apenas arquivos .txt',
    );
  });

  it('rejects non-xlsx file for beneficios mensais', async () => {
    renderWithProviders(<Importacao />);
    const input = getFileInputs()[0] as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['data'], 'beneficios.csv', { type: 'text/csv' })] } });
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));
    expect(toast.error).toHaveBeenCalledWith(
      'Para importação de benefícios mensais, selecione apenas arquivos .xlsx',
    );
  });

  it('imports folha ADP successfully', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: true,
      message: 'Folha importada',
      registrosProcessados: 10,
      erros: [],
      arquivo: 'folha.txt',
      tamanho: 2048,
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(importacaoService.importarFolhaAdp).toHaveBeenCalledWith(
        expect.any(File),
        false,
        false,
      );
    });
    expect(toast.success).toHaveBeenCalledWith('Folha importada');
    expect(screen.getByText(/Importação realizada com sucesso/)).toBeInTheDocument();
    expect(screen.getByText(/Registros processados: 10/)).toBeInTheDocument();
  });

  it('imports folha ADP with decimo terceiro flag', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: true,
      message: 'OK',
      registrosProcessados: 1,
    });

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getAllByRole('checkbox', { name: /13º salário/i })[0]);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(importacaoService.importarFolhaAdp).toHaveBeenCalledWith(expect.any(File), true, false);
    });
  });

  it('shows error when folha ADP import returns failure', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: false,
      message: 'Arquivo inválido',
      arquivo: 'folha.txt',
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Arquivo inválido');
      expect(screen.getByText('Arquivo inválido')).toBeInTheDocument();
    });
  });

  it('alerts when folha ADP reports missing funcionarios', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: false,
      message: 'Funcionários não encontrados: MAT001',
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Funcionários não encontrados: MAT001');
    });
    alertSpy.mockRestore();
  });

  it('opens conflict dialog on folha ADP 409 and confirms substitution', async () => {
    vi.mocked(importacaoService.importarFolhaAdp)
      .mockRejectedValueOnce({ response: { status: 409, data: { message: 'Folha já existe' } } })
      .mockResolvedValueOnce({
        success: true,
        message: 'Substituída',
        registrosProcessados: 5,
        arquivo: 'folha.txt',
        tamanho: 1024,
      });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => expect(screen.getByText('Confirmar Substituição de Folha')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Substituição' }));

    await waitFor(() => {
      expect(importacaoService.importarFolhaAdp).toHaveBeenCalledTimes(2);
      expect(importacaoService.importarFolhaAdp).toHaveBeenLastCalledWith(expect.any(File), false, true);
    });
  });

  it('cancels folha ADP conflict dialog', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockRejectedValue({
      response: { status: 409, data: { message: 'Conflito folha' } },
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows toast when folha file is cleared before import click', async () => {
    renderWithProviders(<Importacao />);
    selectFolhaFile();
    const input = getFileInputs()[1] as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [], configurable: true });
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));
    expect(toast.error).toHaveBeenCalledWith('Por favor, selecione um arquivo');
  });

  it('shows toast when beneficios file is cleared before import click', async () => {
    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    const input = getFileInputs()[0] as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [], configurable: true });
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));
    expect(toast.error).toHaveBeenCalledWith('Por favor, selecione um arquivo');
  });

  it('changes ano select for processamento manual', async () => {
    renderWithProviders(<Importacao />);
    const anoSelects = screen.getAllByRole('combobox', { name: 'Ano' });
    fireEvent.mouseDown(anoSelects[1]);
    const options = screen.getAllByRole('option');
    const anoOption = options.find((o) => o.textContent && /^\d{4}$/.test(o.textContent))!;
    fireEvent.click(anoOption);
    expect(anoSelects[1]).toHaveTextContent(anoOption.textContent!);
  });

  it('shows fallback on confirm substitution error without message', async () => {
    vi.mocked(beneficioMensalService.importar)
      .mockRejectedValueOnce({ response: { status: 409, data: { message: 'Conflito' } } })
      .mockRejectedValueOnce({ response: { data: {} } });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Substituição' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao importar arquivo');
    });
  });

  it('handles folha ADP network error without message', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockRejectedValue({
      response: { data: {} },
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao importar arquivo');
    });
  });

  it('handles folha ADP network error', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockRejectedValue({
      response: { data: { message: 'Erro de rede' } },
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro de rede');
      expect(screen.getByText('Erro de rede')).toBeInTheDocument();
    });
  });

  it('imports beneficios mensais successfully', async () => {
    vi.mocked(beneficioMensalService.importar).mockResolvedValue({
      processadas: 20,
      erros: 0,
      totalValor: 1500.5,
      detalhesErros: [],
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(beneficioMensalService.importar).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Benefícios mensais importados com sucesso!');
    });
    expect(screen.getByText(/Registros processados: 20/)).toBeInTheDocument();
    expect(screen.getByText(/Valor total:/)).toBeInTheDocument();
  });

  it('opens conflict dialog on beneficios 409 and confirms substitution', async () => {
    vi.mocked(beneficioMensalService.importar)
      .mockRejectedValueOnce({ response: { status: 409, data: { message: 'Competência ocupada' } } })
      .mockResolvedValueOnce({
        processadas: 3,
        erros: 0,
        totalValor: 100,
        detalhesErros: [],
      });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByText('Dados já existentes')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Substituição' }));

    await waitFor(() => {
      expect(beneficioMensalService.importar).toHaveBeenCalledTimes(2);
      expect(toast.success).toHaveBeenCalledWith(
        'Benefícios mensais importados com sucesso! Os dados anteriores foram substituídos.',
      );
    });
  });

  it('cancels beneficios conflict dialog', async () => {
    vi.mocked(beneficioMensalService.importar).mockRejectedValue({
      response: { status: 409, data: { message: 'Conflito benefícios' } },
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('alerts on beneficios import validation errors', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.mocked(beneficioMensalService.importar).mockRejectedValue({
      response: { data: { message: 'Erros encontrados: linha 2 inválida' } },
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Erros encontrados: linha 2 inválida');
    });
    alertSpy.mockRestore();
  });

  it('shows fallback error when beneficios import fails without message', async () => {
    vi.mocked(beneficioMensalService.importar).mockRejectedValue({
      response: { data: {} },
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao importar arquivo');
    });
  });

  it('uses default conflict message for beneficios 409 without message', async () => {
    vi.mocked(beneficioMensalService.importar).mockRejectedValue({
      response: { status: 409, data: {} },
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(screen.getByText('Já existem dados para esta competência.')).toBeInTheDocument();
    });
  });

  it('clears file name when file input is cleared', () => {
    renderWithProviders(<Importacao />);
    const input = getFileInputs()[0] as HTMLInputElement;
    selectBeneficiosFile('temp.xlsx');
    expect(screen.getByText(/temp.xlsx/)).toBeInTheDocument();
    fireEvent.change(input, { target: { files: [] } });
    expect(screen.queryByText(/temp.xlsx/)).not.toBeInTheDocument();
  });

  it('opens file picker when select buttons are clicked', () => {
    renderWithProviders(<Importacao />);
    const beneficiosInput = getFileInputs()[0] as HTMLInputElement;
    const folhaInput = getFileInputs()[1] as HTMLInputElement;
    const clickSpy = vi.spyOn(beneficiosInput, 'click');
    const folhaClickSpy = vi.spyOn(folhaInput, 'click');

    fireEvent.click(screen.getByRole('button', { name: 'Selecionar Arquivo (.xlsx)' }));
    fireEvent.click(screen.getByRole('button', { name: 'Selecionar Arquivo (.txt)' }));

    expect(clickSpy).toHaveBeenCalled();
    expect(folhaClickSpy).toHaveBeenCalled();
  });

  it('closes help dialog via escape key', async () => {
    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByTestId('HelpOutlineIcon').closest('button')!);
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape', code: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows toast on confirm substitution generic error for beneficios', async () => {
    vi.mocked(beneficioMensalService.importar)
      .mockRejectedValueOnce({ response: { status: 409, data: { message: 'Conflito' } } })
      .mockRejectedValueOnce({ response: { data: { message: 'Falha genérica' } } });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Substituição' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Falha genérica');
    });
  });

  it('shows loading text during beneficios import', async () => {
    let resolveImport!: (value: unknown) => void;
    vi.mocked(beneficioMensalService.importar).mockImplementation(
      () => new Promise((resolve) => { resolveImport = resolve; }),
    );

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(screen.getByText('Processando arquivo...')).toBeInTheDocument();
    });

    resolveImport({ processadas: 1, erros: 0, totalValor: 0, detalhesErros: [] });
  });

  it('processes manual competencia with message error', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockRejectedValue({
      response: { data: { message: 'Erro no processamento' } },
    });

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro no processamento');
    });
  });

  it('clears folha file name when file input is cleared', () => {
    renderWithProviders(<Importacao />);
    const input = getFileInputs()[1] as HTMLInputElement;
    selectFolhaFile('temp.txt');
    expect(screen.getByText(/temp.txt/)).toBeInTheDocument();
    fireEvent.change(input, { target: { files: [] } });
    expect(screen.queryByText(/temp.txt/)).not.toBeInTheDocument();
  });

  it('shows toast on generic beneficios import error', async () => {
    vi.mocked(beneficioMensalService.importar).mockRejectedValue({
      response: { data: { message: 'Falha na importação' } },
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Falha na importação');
    });
  });

  it('shows error list after beneficios import with detalhes', async () => {
    vi.mocked(beneficioMensalService.importar).mockResolvedValue({
      processadas: 1,
      erros: 1,
      totalValor: 50,
      detalhesErros: ['Linha 3: CPF inválido'],
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => {
      expect(screen.getByText('Linha 3: CPF inválido')).toBeInTheDocument();
    });
  });

  it('shows folha ADP error list after import', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: true,
      message: 'OK',
      registrosProcessados: 2,
      erros: ['Linha 5: rubrica desconhecida'],
      arquivo: 'folha.txt',
      tamanho: 512,
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(screen.getByText('Linha 5: rubrica desconhecida')).toBeInTheDocument();
    });
  });

  it('resets folha ADP state with Novo button', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockResolvedValue({
      success: true,
      message: 'OK',
      registrosProcessados: 1,
      arquivo: 'folha.txt',
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile('minha-folha.txt');
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo' }));
    expect(screen.queryByText(/Importação realizada com sucesso/)).not.toBeInTheDocument();
  });

  it('resets beneficios state with Novo button', async () => {
    vi.mocked(beneficioMensalService.importar).mockResolvedValue({
      processadas: 1,
      erros: 0,
      totalValor: 10,
      detalhesErros: [],
    });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile('planilha.xlsx');
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Novo' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Novo' }));
    expect(screen.queryByText(/Importação realizada com sucesso/)).not.toBeInTheDocument();
  });

  it('displays selected file names', () => {
    renderWithProviders(<Importacao />);
    selectBeneficiosFile('beneficios-jan.xlsx');
    selectFolhaFile('folha-jan.txt');
    expect(screen.getByText(/beneficios-jan.xlsx/)).toBeInTheDocument();
    expect(screen.getByText(/folha-jan.txt/)).toBeInTheDocument();
  });

  it('processes manual competencia successfully', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockResolvedValue({
      totalFichas: 1,
      totalLinhas: 5,
      totalFuncionarios: 1,
    });

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(folhaPagamentoService.processarCompetencia).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Ficha processada: 1 fichas, 5 linhas');
    });
  });

  it('shows 403 error when processing without permission', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockRejectedValue({
      response: { status: 403 },
    });

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Você não tem permissão para processar a ficha. Apenas administradores podem executar esta ação.',
      );
    });
  });

  it('shows detail message when processamento fails', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockRejectedValue({
      response: { data: { detail: 'Competência não encontrada' } },
    });

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Competência não encontrada');
    });
  });

  it('shows fallback error when processamento fails without message', async () => {
    vi.mocked(folhaPagamentoService.processarCompetencia).mockRejectedValue(new Error('fail'));

    renderWithProviders(<Importacao />);
    fireEvent.click(screen.getByRole('button', { name: 'Processar' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Erro ao processar ficha da competência');
    });
  });

  it('toggles processamento checkboxes and changes month select', async () => {
    renderWithProviders(<Importacao />);

    const checkboxes = screen.getAllByRole('checkbox', { name: /13º salário/i });
    fireEvent.click(checkboxes[1]);
    expect(checkboxes[1]).toBeChecked();

    fireEvent.click(screen.getByRole('checkbox', { name: /Recalcular férias proporcionais/i }));
    expect(screen.getByRole('checkbox', { name: /Recalcular férias proporcionais/i })).toBeChecked();

    const mesSelects = screen.getAllByRole('combobox', { name: 'Mês' });
    fireEvent.mouseDown(mesSelects[0]);
    fireEvent.click(screen.getByRole('option', { name: 'Fevereiro' }));

    fireEvent.mouseDown(mesSelects[1]);
    const fevereiroOptions = screen.getAllByRole('option', { name: 'Fevereiro' });
    fireEvent.click(fevereiroOptions[fevereiroOptions.length - 1]);
  });

  it('changes ano select for beneficios import', async () => {
    renderWithProviders(<Importacao />);
    const anoSelects = screen.getAllByRole('combobox', { name: 'Ano' });
    fireEvent.mouseDown(anoSelects[0]);
    const options = screen.getAllByRole('option');
    const anoOption = options.find((o) => o.textContent && /^\d{4}$/.test(o.textContent))!;
    fireEvent.click(anoOption);
    expect(anoSelects[0]).toHaveTextContent(anoOption.textContent!);
  });

  it('handles confirm substitution error for beneficios', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.mocked(beneficioMensalService.importar)
      .mockRejectedValueOnce({ response: { status: 409, data: { message: 'Conflito' } } })
      .mockRejectedValueOnce({
        response: { data: { message: 'Erros encontrados: validação' } },
      });

    renderWithProviders(<Importacao />);
    selectBeneficiosFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Benefícios Mensais' }));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Substituição' }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Erros encontrados: validação');
    });
    alertSpy.mockRestore();
  });

  it('uses default conflict message when 409 has no message', async () => {
    vi.mocked(importacaoService.importarFolhaAdp).mockRejectedValue({
      response: { status: 409, data: {} },
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(screen.getByText('Já existe uma folha para este período.')).toBeInTheDocument();
    });
  });

  it('alerts on folha catch with missing funcionarios message', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.mocked(importacaoService.importarFolhaAdp).mockRejectedValue({
      response: { data: { message: 'Funcionários não encontrados: X' } },
    });

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Funcionários não encontrados: X');
    });
    alertSpy.mockRestore();
  });

  it('shows loading text during folha import', async () => {
    let resolveImport!: (value: unknown) => void;
    vi.mocked(importacaoService.importarFolhaAdp).mockImplementation(
      () => new Promise((resolve) => { resolveImport = resolve; }),
    );

    renderWithProviders(<Importacao />);
    selectFolhaFile();
    fireEvent.click(screen.getByRole('button', { name: 'Importar Folha ADP' }));

    await waitFor(() => {
      expect(screen.getAllByText('Importando e processando ficha…').length).toBeGreaterThan(0);
    });

    resolveImport({
      success: true,
      message: 'OK',
      registrosProcessados: 1,
    });
  });
});
