import api from './api';
import type { NoOrganograma, NoOrganogramaFormData, FuncionarioOrganograma, CentroCustoOrganograma, OrganogramaTree } from '../types';

export const organogramaService = {
  // Operações de Nós
  listarTodos: async (): Promise<NoOrganograma[]> => {
    const response = await api.get('/organograma');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<NoOrganograma> => {
    const response = await api.get(`/organograma/${id}`);
    return response.data;
  },

  criarNo: async (data: NoOrganogramaFormData): Promise<NoOrganograma> => {
    const response = await api.post('/organograma', data);
    return response.data;
  },

  atualizarNo: async (id: number, data: Partial<NoOrganogramaFormData>): Promise<NoOrganograma> => {
    const response = await api.put(`/organograma/${id}`, data);
    return response.data;
  },

  removerNo: async (id: number): Promise<void> => {
    await api.delete(`/organograma/${id}`);
  },

  // Operações de estrutura hierárquica
  obterArvore: async (): Promise<OrganogramaTree[]> => {
    const response = await api.get('/organograma/arvore');
    return response.data;
  },

  moverNo: async (noId: number, novoParentId?: number, novaPosicao?: number): Promise<NoOrganograma> => {
    const params = new URLSearchParams();
    if (novoParentId !== undefined) params.append('novoParentId', novoParentId.toString());
    if (novaPosicao !== undefined) params.append('novaPosicao', novaPosicao.toString());
    
    const response = await api.put(`/organograma/${noId}/mover?${params.toString()}`);
    return response.data;
  },

  // Operações de Funcionários
  adicionarFuncionario: async (noId: number, funcionarioId: number): Promise<FuncionarioOrganograma> => {
    const response = await api.post(`/organograma/${noId}/funcionarios/${funcionarioId}`);
    return response.data;
  },

  removerFuncionario: async (noId: number, funcionarioId: number): Promise<void> => {
    await api.delete(`/organograma/${noId}/funcionarios/${funcionarioId}`);
  },

  listarFuncionarios: async (noId: number): Promise<FuncionarioOrganograma[]> => {
    const response = await api.get(`/organograma/${noId}/funcionarios`);
    return response.data;
  },

  // Operações de Centros de Custo
  adicionarCentroCusto: async (noId: number, centroCustoId: number): Promise<CentroCustoOrganograma> => {
    const response = await api.post(`/organograma/${noId}/centros-custo/${centroCustoId}`);
    return response.data;
  },

  removerCentroCusto: async (noId: number, centroCustoId: number): Promise<void> => {
    await api.delete(`/organograma/${noId}/centros-custo/${centroCustoId}`);
  },

  listarCentrosCusto: async (noId: number): Promise<CentroCustoOrganograma[]> => {
    const response = await api.get(`/organograma/${noId}/centros-custo`);
    return response.data;
  }
}; 