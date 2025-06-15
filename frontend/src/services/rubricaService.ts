import api from './api';
import type { Rubrica } from '../types';

export const rubricaService = {
  listar: async () => {
    const response = await api.get<Rubrica[]>('/rubricas');
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<Rubrica>(`/rubricas/${id}`);
    return response.data;
  },

  criar: async (rubrica: Omit<Rubrica, 'id'>) => {
    const response = await api.post<Rubrica>('/rubricas', rubrica);
    return response.data;
  },

  atualizar: async (id: number, rubrica: Partial<Rubrica>) => {
    const response = await api.put<Rubrica>(`/rubricas/${id}`, rubrica);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/rubricas/${id}`);
  },
}; 