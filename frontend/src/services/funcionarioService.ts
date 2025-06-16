import api from './api';
import type { Funcionario } from '../types';

export const funcionarioService = {
  listar: async () => {
    const response = await api.get<Funcionario[]>('/funcionarios');
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<Funcionario>(`/funcionarios/${id}`);
    return response.data;
  },

  criar: async (funcionario: Omit<Funcionario, 'id'>) => {
    const response = await api.post<Funcionario>('/funcionarios', funcionario);
    return response.data;
  },

  atualizar: async (id: number, funcionario: Partial<Funcionario>) => {
    const response = await api.put<Funcionario>(`/funcionarios/${id}`, funcionario);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/funcionarios/${id}`);
  },
}; 