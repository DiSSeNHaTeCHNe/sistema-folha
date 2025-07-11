import api from './api';
import type { Usuario } from '../types';

export interface UsuarioForm {
  login: string;
  nome: string;
  senha?: string;
  funcionarioId?: number;
  permissoes: string[];
}

export interface UsuarioFiltros {
  nome?: string;
  login?: string;
  funcionarioId?: number;
}

export const usuarioService = {
  // Listar todos os usuários
  listar: async (filtros?: UsuarioFiltros): Promise<Usuario[]> => {
    const params = new URLSearchParams();
    if (filtros?.nome) params.append('nome', filtros.nome);
    if (filtros?.login) params.append('login', filtros.login);
    if (filtros?.funcionarioId) params.append('funcionarioId', filtros.funcionarioId.toString());

    const response = await api.get(`/usuarios?${params.toString()}`);
    return response.data;
  },

  // Buscar usuário por ID
  buscarPorId: async (id: number): Promise<Usuario> => {
    const response = await api.get(`/usuarios/${id}`);
    return response.data;
  },

  // Buscar usuário por login
  buscarPorLogin: async (login: string): Promise<Usuario> => {
    const response = await api.get(`/usuarios/login/${login}`);
    return response.data;
  },

  // Criar novo usuário
  criar: async (dados: UsuarioForm): Promise<Usuario> => {
    const response = await api.post('/usuarios', dados);
    return response.data;
  },

  // Atualizar usuário
  atualizar: async (id: number, dados: UsuarioForm): Promise<Usuario> => {
    const response = await api.put(`/usuarios/${id}`, dados);
    return response.data;
  },

  // Excluir usuário
  excluir: async (id: number): Promise<void> => {
    await api.delete(`/usuarios/${id}`);
  },

  // Alterar senha
  alterarSenha: async (id: number, senhaAtual: string, novaSenha: string): Promise<void> => {
    await api.put(`/usuarios/${id}/senha`, {
      senhaAtual,
      novaSenha
    });
  },

  // Listar permissões disponíveis
  listarPermissoes: async (): Promise<string[]> => {
    const response = await api.get('/usuarios/permissoes');
    return response.data;
  },

  // Listar funcionários para vincular
  listarFuncionarios: async (): Promise<{id: number, nome: string, cpf: string}[]> => {
    const response = await api.get('/funcionarios');
    return response.data.map((f: any) => ({
      id: f.id,
      nome: f.nome,
      cpf: f.cpf
    }));
  }
};

export default usuarioService; 