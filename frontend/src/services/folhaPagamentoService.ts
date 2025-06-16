import api from './api';
import type { FolhaPagamento } from '../types';

export const folhaPagamentoService = {
  listar: async () => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento');
    return response.data;
  },

  buscarPorFuncionario: async (funcionarioId: number, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/funcionario/${funcionarioId}`, {
      params: { dataInicio, dataFim },
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

  buscarPorPeriodo: async (dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento', {
      params: { dataInicio, dataFim },
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
}; 