import api from './api';
import type { Funcionario } from '../types';

export interface FuncionarioCreatePayload {
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId?: number;
  centroCustoId?: number;
  idExterno?: string;
}

export type FuncionarioUpdatePayload = Partial<FuncionarioCreatePayload>;

export type FuncionarioStatusFiltro = 'ATIVO' | 'INATIVO' | 'TODOS';

export const funcionarioService = {
  listar: async () => {
    const response = await api.get<Funcionario[]>('/funcionarios?status=ATIVO');
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<Funcionario>(`/funcionarios/${id}`);
    return response.data;
  },

  criar: async (funcionario: FuncionarioCreatePayload) => {
    const response = await api.post<Funcionario>('/funcionarios', funcionario);
    return response.data;
  },

  atualizar: async (id: number, funcionario: FuncionarioUpdatePayload) => {
    const response = await api.put<Funcionario>(`/funcionarios/${id}`, funcionario);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/funcionarios/${id}`);
  },

  filtrar: async (filtros: {
    nome?: string;
    cargoId?: string;
    centroCustoId?: string;
    linhaNegocioId?: string;
    status?: FuncionarioStatusFiltro;
  }) => {
    const params = new URLSearchParams();
    if (filtros.nome) params.append('nome', filtros.nome);
    if (filtros.cargoId && filtros.cargoId !== '') params.append('cargoId', filtros.cargoId);
    if (filtros.centroCustoId && filtros.centroCustoId !== '') {
      params.append('centroCustoId', filtros.centroCustoId);
    }
    if (filtros.linhaNegocioId && filtros.linhaNegocioId !== '') {
      params.append('linhaNegocioId', filtros.linhaNegocioId);
    }
    if (filtros.status) {
      params.append('status', filtros.status);
    }
    const query = params.toString();
    const url = query ? `/funcionarios?${query}` : '/funcionarios';
    const response = await api.get<Funcionario[]>(url);
    return response.data;
  },
}; 