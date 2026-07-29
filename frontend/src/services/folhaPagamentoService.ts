import api from './api';
import type { FolhaPagamento } from '../types';

export type TotalizadorFolha = 'GROSS' | 'NET' | 'COMPANY_COST';

export interface FichaLinhaDetalhe {
  valor: string | number;
  contribuicao: string | number;
  origemLinha: string;
  rubricaCodigo: string;
  rubricaDescricao: string;
  porcentagem?: string | number | null;
}

export interface FolhaTotaisFuncionario {
  funcionarioId: number;
  funcionarioNome: string;
  competenciaInicio: string;
  competenciaFim: string;
  cargoId?: number;
  cargoDescricao?: string;
  centroCustoId?: number;
  centroCustoDescricao?: string;
  linhaNegocioId?: number;
  linhaNegocioDescricao?: string;
  totalRubricas: number;
  totalBeneficios: number;
  salBruto: string | number;
  salLiquido: string | number;
  salCustoFolha: string | number;
  salCustoBeneficios: string | number;
  encargosRateados: string | number;
  custoEmpresa: string | number;
}

export interface ProcessamentoResultado {
  totalFichas: number;
  totalLinhas: number;
  totalFuncionarios: number;
}

export const folhaPagamentoService = {
  listar: async () => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento');
    return response.data;
  },

  buscarPorFuncionario: async (
    funcionarioId: number,
    dataInicio: string,
    dataFim: string,
    decimoTerceiro?: boolean,
  ) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/funcionario/${funcionarioId}`, {
      params: { dataInicio, dataFim, decimoTerceiro },
    });
    return response.data;
  },

  buscarPorCentroCusto: async (centroCusto: string, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/centro-custo/${centroCusto}`, {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  buscarPorLinhaNegocio: async (linhaNegocio: string, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/linha-negocio/${linhaNegocio}`, {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  buscarPorPeriodo: async (dataInicio: string, dataFim: string, decimoTerceiro?: boolean) => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento', {
      params: { dataInicio, dataFim, decimoTerceiro },
    });
    return response.data;
  },

  consultarTotaisPorFuncionario: async (
    dataInicio: string,
    dataFim: string,
    decimoTerceiro?: boolean,
  ): Promise<FolhaTotaisFuncionario[]> => {
    const response = await api.get<FolhaTotaisFuncionario[]>('/folha-pagamento/totais-funcionarios', {
      params: { dataInicio, dataFim, decimoTerceiro },
    });
    return response.data;
  },

  buscarFichaPorFuncionario: async (
    funcionarioId: number,
    dataInicio: string,
    dataFim: string,
    decimoTerceiro?: boolean,
  ): Promise<number | null> => {
    try {
      const response = await api.get<{ id: number }>('/folha-pagamento/fichas/por-funcionario', {
        params: { funcionarioId, dataInicio, dataFim, decimoTerceiro },
      });
      return response.data.id;
    } catch {
      return null;
    }
  },

  listarLinhasPorTotalizador: async (
    fichaId: number,
    totalizer: TotalizadorFolha,
  ): Promise<FichaLinhaDetalhe[]> => {
    const response = await api.get<FichaLinhaDetalhe[]>(`/folha-pagamento/fichas/${fichaId}/linhas`, {
      params: { totalizer },
    });
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<FolhaPagamento>(`/folha-pagamento/${id}`);
    return response.data;
  },

  criar: async (folhaPagamento: Omit<FolhaPagamento, 'id'>) => {
    const response = await api.post<FolhaPagamento>('/folha-pagamento', folhaPagamento);
    return response.data;
  },

  atualizar: async (id: number, folhaPagamento: Partial<FolhaPagamento>) => {
    const response = await api.put<FolhaPagamento>(`/folha-pagamento/${id}`, folhaPagamento);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/folha-pagamento/${id}`);
  },

  importar: async (arquivo: File) => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    const response = await api.post<FolhaPagamento[]>('/folha-pagamento/importar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  processarCompetencia: async (params: {
    competenciaInicio: string;
    competenciaFim: string;
    decimoTerceiro: boolean;
    recalcularFerias: boolean;
  }): Promise<ProcessamentoResultado> => {
    const response = await api.post<ProcessamentoResultado>('/folha-pagamento/processar', {
      competenciaInicio: params.competenciaInicio,
      competenciaFim: params.competenciaFim,
      decimoTerceiro: params.decimoTerceiro,
      opcoes: { recalcularFerias: params.recalcularFerias },
    });
    return response.data;
  },
};
