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
  ativo: boolean;
}

const resumoFolhaPagamentoService = {
  listarTodos: async (): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento');
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