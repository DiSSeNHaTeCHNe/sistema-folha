import api from './api';
import type { CentroCusto } from '../types';

interface CentroCustoFormData {
  descricao: string;
  linhaNegocioId: number;
}

const centroCustoService = {
  listarTodos: async (): Promise<CentroCusto[]> => {
    const response = await api.get('/centros-custo');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<CentroCusto> => {
    const response = await api.get(`/centros-custo/${id}`);
    return response.data;
  },

  listarPorLinhaNegocio: async (linhaNegocioId: number): Promise<CentroCusto[]> => {
    const response = await api.get(`/centros-custo/linha-negocio/${linhaNegocioId}`);
    return response.data;
  },

  cadastrar: async (data: CentroCustoFormData): Promise<CentroCusto> => {
    const response = await api.post('/centros-custo', data);
    return response.data;
  },

  atualizar: async (id: number, data: CentroCustoFormData): Promise<CentroCusto> => {
    const response = await api.put(`/centros-custo/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/centros-custo/${id}`);
  }
};

export { centroCustoService }; 