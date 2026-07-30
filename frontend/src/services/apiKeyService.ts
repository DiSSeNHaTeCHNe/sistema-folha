import api from './api';

export interface ApiKeyCreateRequest {
  nome: string;
  diasValidade?: number;
}

export interface ApiKeyCreated {
  id: number;
  nome: string;
  prefixo: string;
  chave: string;
  dataExpiracao: string;
  escopo: string;
  dataCriacao: string;
}

export interface ApiKeyListItem {
  id: number;
  nome: string;
  prefixo: string;
  dataExpiracao: string;
  revogado: boolean;
  escopo: string;
  ultimoUsoEm: string | null;
  dataCriacao: string;
}

export const apiKeyService = {
  criar: async (dados: ApiKeyCreateRequest): Promise<ApiKeyCreated> => {
    const response = await api.post<ApiKeyCreated>('/auth/api-keys', dados);
    return response.data;
  },

  listar: async (usuarioId?: number): Promise<ApiKeyListItem[]> => {
    const params = usuarioId != null ? `?usuarioId=${usuarioId}` : '';
    const response = await api.get<ApiKeyListItem[]>(`/auth/api-keys${params}`);
    return response.data;
  },

  revogar: async (id: number): Promise<void> => {
    await api.delete(`/auth/api-keys/${id}`);
  },
};

export default apiKeyService;
