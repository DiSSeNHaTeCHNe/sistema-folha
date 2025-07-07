import api from './api';
import type { Cargo } from '../types';

interface CargoFormData {
  descricao: string;
}

const cargoService = {
  listarTodos: async (): Promise<Cargo[]> => {
    const response = await api.get('/cargos');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Cargo> => {
    const response = await api.get(`/cargos/${id}`);
    return response.data;
  },

  cadastrar: async (data: CargoFormData): Promise<Cargo> => {
    const response = await api.post('/cargos', data);
    return response.data;
  },

  atualizar: async (id: number, data: CargoFormData): Promise<Cargo> => {
    const response = await api.put(`/cargos/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/cargos/${id}`);
  }
};

export { cargoService }; 