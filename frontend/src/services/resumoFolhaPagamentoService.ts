import api from './api';

export interface ResumoFolhaPagamento {
  id: number;
  totalEmpregados: number;
  totalEncargos: number;
  totalPagamentos: number;
  totalDescontos: number;
  totalLiquido: number;
  competenciaInicio: string;
  competenciaFim: string;
  dataImportacao: string;
  decimoTerceiro: boolean;
  ativo: boolean;
}

const resumoFolhaPagamentoService = {
  listarPorAno: async (ano: number, mes?: number): Promise<ResumoFolhaPagamento[]> => {
    const params: { ano: number; mes?: number } = { ano };
    if (mes !== undefined) {
      params.mes = mes;
    }
    const response = await api.get('/resumo-folha-pagamento', { params });
    return response.data;
  },

  buscarPorPeriodo: async (dataInicio: string, dataFim: string): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento/periodo', {
      params: { dataInicio, dataFim }
    });
    return response.data;
  },

  buscarPorCompetencia: async (competenciaInicio: string, competenciaFim: string): Promise<ResumoFolhaPagamento> => {
    const response = await api.get('/resumo-folha-pagamento/competencia', {
      params: { competenciaInicio, competenciaFim }
    });
    return response.data;
  },

  listarMaisRecentes: async (): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento/latest');
    return response.data;
  }
};

export { resumoFolhaPagamentoService }; 