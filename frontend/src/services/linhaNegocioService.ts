import api from './api';
import type { LinhaNegocio } from '../types';

interface LinhaNegocioFormData {
  descricao: string;
}

const linhaNegocioService = {
  listarTodos: async (): Promise<LinhaNegocio[]> => {
    const response = await api.get('/linhas-negocio');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<LinhaNegocio> => {
    const response = await api.get(`/linhas-negocio/${id}`);
    return response.data;
  },

  cadastrar: async (data: LinhaNegocioFormData): Promise<LinhaNegocio> => {
    const response = await api.post('/linhas-negocio', data);
    return response.data;
  },

  atualizar: async (id: number, data: LinhaNegocioFormData): Promise<LinhaNegocio> => {
    const response = await api.put(`/linhas-negocio/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/linhas-negocio/${id}`);
  }
};

export { linhaNegocioService }; 