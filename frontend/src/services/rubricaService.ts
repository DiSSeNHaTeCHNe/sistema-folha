import api from './api';
import type { Rubrica } from '../types';

interface RubricaFormData {
  codigo: string;
  descricao: string;
  tipo: 'PROVENTO' | 'DESCONTO' | 'INFORMATIVO';
  porcentagem?: number;
}

const rubricaService = {
  listarTodos: async (): Promise<Rubrica[]> => {
    const response = await api.get('/rubricas');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Rubrica> => {
    const response = await api.get(`/rubricas/${id}`);
    return response.data;
  },

  cadastrar: async (data: RubricaFormData): Promise<Rubrica> => {
    const response = await api.post('/rubricas', data);
    return response.data;
  },

  atualizar: async (id: number, data: RubricaFormData): Promise<Rubrica> => {
    const response = await api.put(`/rubricas/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/rubricas/${id}`);
  }
};

export { rubricaService }; 