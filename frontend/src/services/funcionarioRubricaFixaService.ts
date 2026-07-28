import api from './api';

export interface FuncionarioRubricaFixa {
  id?: number;
  funcionarioId: number;
  rubricaId: number;
  valor?: string | number | null;
  vigenciaInicio: string;
  vigenciaFim?: string | null;
  comentario?: string | null;
  ativo?: boolean;
  funcionarioNome?: string;
  rubricaCodigo?: string;
  rubricaDescricao?: string;
}

export interface FuncionarioRubricaFixaFiltros {
  funcionarioId?: number | '';
  rubricaId?: number | '';
}

export interface FuncionarioRubricaFixaFormData {
  funcionarioId: number;
  rubricaId: number;
  valor?: string;
  vigenciaInicio: string;
  vigenciaFim?: string;
  comentario?: string;
}

const funcionarioRubricaFixaService = {
  listar: async (filtros?: FuncionarioRubricaFixaFiltros): Promise<FuncionarioRubricaFixa[]> => {
    const params = new URLSearchParams();
    if (filtros?.funcionarioId) {
      params.append('funcionarioId', String(filtros.funcionarioId));
    }
    if (filtros?.rubricaId) {
      params.append('rubricaId', String(filtros.rubricaId));
    }
    const query = params.toString();
    const url = query ? `/funcionario-rubrica-fixa?${query}` : '/funcionario-rubrica-fixa';
    const response = await api.get<FuncionarioRubricaFixa[]>(url);
    return response.data;
  },

  criar: async (data: FuncionarioRubricaFixaFormData): Promise<FuncionarioRubricaFixa> => {
    const payload = {
      funcionarioId: data.funcionarioId,
      rubricaId: data.rubricaId,
      valor: data.valor ? data.valor.replace(',', '.') : null,
      vigenciaInicio: data.vigenciaInicio,
      vigenciaFim: data.vigenciaFim || null,
      comentario: data.comentario || null,
    };
    const response = await api.post<FuncionarioRubricaFixa>('/funcionario-rubrica-fixa', payload);
    return response.data;
  },

  atualizar: async (id: number, data: FuncionarioRubricaFixaFormData): Promise<FuncionarioRubricaFixa> => {
    const payload = {
      funcionarioId: data.funcionarioId,
      rubricaId: data.rubricaId,
      valor: data.valor ? data.valor.replace(',', '.') : null,
      vigenciaInicio: data.vigenciaInicio,
      vigenciaFim: data.vigenciaFim || null,
      comentario: data.comentario || null,
    };
    const response = await api.put<FuncionarioRubricaFixa>(`/funcionario-rubrica-fixa/${id}`, payload);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/funcionario-rubrica-fixa/${id}`);
  },
};

export { funcionarioRubricaFixaService };
