import api from './api';

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
}

export const relatorioService = {
  async gerarRelatorioFolha(mes: number, ano: number): Promise<RelatorioFolha> {
    const response = await api.post<RelatorioFolha>('/relatorios/folha', { mes, ano });
    return response.data;
  },

  async gerarRelatorioBeneficio(mes: number, ano: number): Promise<RelatorioBeneficio> {
    const response = await api.post<RelatorioBeneficio>('/relatorios/beneficio', { mes, ano });
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