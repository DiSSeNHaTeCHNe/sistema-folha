import api from './api';
import type { Rubrica } from '../types';

interface RubricaFormData {
  codigo: string;
  descricao: string;
  tipo: string;
  porcentagem?: number;
}

export type RubricaStatusFiltro = 'ATIVO' | 'INATIVO' | 'TODOS';

export interface RubricaFiltros {
  codigo?: string;
  descricao?: string;
  status?: RubricaStatusFiltro;
}

const mapRubrica = (item: {
  id: number;
  codigo: string;
  descricao: string;
  tipo: Rubrica['tipo'];
  tipoRubricaDescricao?: string;
  porcentagem?: number;
  ativo: boolean;
}): Rubrica => ({
  ...item,
  tipo: item.tipoRubricaDescricao
    ? (item.tipoRubricaDescricao as Rubrica['tipo'])
    : item.tipo,
  tipoRubricaDescricao: item.tipoRubricaDescricao,
});

const rubricaService = {
  listar: async (filtros?: RubricaFiltros): Promise<Rubrica[]> => {
    const params = new URLSearchParams();
    const codigo = filtros?.codigo?.trim();
    const descricao = filtros?.descricao?.trim();
    if (codigo) params.append('codigo', codigo);
    if (descricao) params.append('descricao', descricao);
    params.append('status', filtros?.status ?? 'ATIVO');

    const response = await api.get(`/rubricas?${params.toString()}`);
    return response.data.map(mapRubrica);
  },

  buscarPorId: async (id: number): Promise<Rubrica> => {
    const response = await api.get(`/rubricas/${id}`);
    return mapRubrica(response.data);
  },

  cadastrar: async (data: RubricaFormData): Promise<Rubrica> => {
    const payload = {
      ...data,
      tipoRubricaDescricao: data.tipo
    };
    const response = await api.post('/rubricas', payload);
    return response.data;
  },

  atualizar: async (id: number, data: RubricaFormData): Promise<Rubrica> => {
    const payload = {
      ...data,
      tipoRubricaDescricao: data.tipo
    };
    const response = await api.put(`/rubricas/${id}`, payload);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/rubricas/${id}`);
  }
};

export { rubricaService };
