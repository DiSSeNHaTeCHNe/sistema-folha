import api from './api';
import type { Beneficio } from '../types';

export const beneficioService = {
  listar: async () => {
    const response = await api.get<Beneficio[]>('/beneficios');
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<Beneficio>(`/beneficios/${id}`);
    return response.data;
  },

  criar: async (beneficio: Omit<Beneficio, 'id'>) => {
    const response = await api.post<Beneficio>('/beneficios', beneficio);
    return response.data;
  },

  atualizar: async (id: number, beneficio: Partial<Beneficio>) => {
    const response = await api.put<Beneficio>(`/beneficios/${id}`, beneficio);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/beneficios/${id}`);
  },

  importar: async (arquivo: File) => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    const response = await api.post<Beneficio[]>('/beneficios/importar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
}; 