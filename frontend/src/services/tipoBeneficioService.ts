import api from './api';
import type { TipoBeneficio } from '../types';

export const tipoBeneficioService = {
  listar: async (): Promise<TipoBeneficio[]> => {
    const response = await api.get<TipoBeneficio[]>('/tipo-beneficio');
    return response.data;
  },

  criar: async (dto: Omit<TipoBeneficio, 'id'>): Promise<TipoBeneficio> => {
    const response = await api.post<TipoBeneficio>('/tipo-beneficio', dto);
    return response.data;
  },

  atualizar: async (id: number, dto: Partial<TipoBeneficio>): Promise<TipoBeneficio> => {
    const response = await api.put<TipoBeneficio>(`/tipo-beneficio/${id}`, dto);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/tipo-beneficio/${id}`);
  },
};
