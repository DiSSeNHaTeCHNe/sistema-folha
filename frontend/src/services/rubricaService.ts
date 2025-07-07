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
    // Mapeia os dados para garantir compatibilidade
    return response.data.map((item: any) => ({
      ...item,
      tipo: item.tipo || item.tipoRubricaDescricao
    }));
  },

  buscarPorId: async (id: number): Promise<Rubrica> => {
    const response = await api.get(`/rubricas/${id}`);
    const item = response.data;
    return {
      ...item,
      tipo: item.tipo || item.tipoRubricaDescricao
    };
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