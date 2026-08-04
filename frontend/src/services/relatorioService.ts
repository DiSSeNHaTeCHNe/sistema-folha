import api from './api';
import axios from 'axios';

/** Backend aguarda até 60s (`relatorios.geracao.timeout-segundos`); axios default é 10s. */
export const RELATORIO_GERACAO_TIMEOUT_MS = 65_000;

export interface RelatorioFolha {
  id: number;
  mes: number;
  ano: number;
  totalFuncionarios: number;
  totalFolha: number;
  totalBeneficios: number;
  status: 'PENDENTE' | 'PROCESSADO' | 'ERRO';
  dataProcessamento?: string;
  erro?: string;
  dataCriacao?: string;
  stale?: boolean;
}

export interface RelatorioBeneficio {
  id: number;
  mes: number;
  ano: number;
  totalBeneficios: number;
  totalValor: number;
  status: 'PENDENTE' | 'PROCESSADO' | 'ERRO';
  dataProcessamento?: string;
  erro?: string;
  dataCriacao?: string;
  stale?: boolean;
}

interface ApiErrorBody {
  message?: string;
  detail?: string;
}

function isTimeoutError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return false;
  }
  return error.code === 'ECONNABORTED' || error.message.toLowerCase().includes('timeout');
}

export function resolveRelatorioApiError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const status = error.response?.status;
    if (status === 429) {
      return error.response?.data?.message
        ?? 'Limite de gerações simultâneas atingido. Aguarde a conclusão dos relatórios em andamento.';
    }
    if (status === 403) {
      return error.response?.data?.message ?? 'Acesso negado para gerar relatórios.';
    }
    if (isTimeoutError(error)) {
      return 'Tempo esgotado na requisição. O relatório pode continuar sendo gerado — aguarde ou atualize a página.';
    }
    if (error.response?.data?.message) {
      return error.response.data.message;
    }
  }
  return 'Erro ao gerar relatório';
}

export const relatorioService = {
  async gerarRelatorioFolha(mes: number, ano: number): Promise<RelatorioFolha> {
    const response = await api.post<RelatorioFolha>(
      '/relatorios/folha',
      { mes, ano },
      { timeout: RELATORIO_GERACAO_TIMEOUT_MS },
    );
    return response.data;
  },

  async gerarRelatorioBeneficio(mes: number, ano: number): Promise<RelatorioBeneficio> {
    const response = await api.post<RelatorioBeneficio>(
      '/relatorios/beneficio',
      { mes, ano },
      { timeout: RELATORIO_GERACAO_TIMEOUT_MS },
    );
    return response.data;
  },

  async listarRelatoriosFolha(): Promise<RelatorioFolha[]> {
    const response = await api.get<RelatorioFolha[]>('/relatorios/folha');
    return response.data;
  },

  async listarRelatoriosBeneficio(): Promise<RelatorioBeneficio[]> {
    const response = await api.get<RelatorioBeneficio[]>('/relatorios/beneficio');
    return response.data;
  },

  async downloadRelatorioFolha(id: number): Promise<Blob> {
    const response = await api.get(`/relatorios/folha/${id}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async downloadRelatorioBeneficio(id: number): Promise<Blob> {
    const response = await api.get(`/relatorios/beneficio/${id}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
