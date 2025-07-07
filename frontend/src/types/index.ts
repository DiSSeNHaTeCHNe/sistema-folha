export interface Usuario {
  id: number;
  login: string;
  nome: string;
  centroCusto: string;
  permissoes: string[];
  primeiroAcesso: boolean;
}

export interface Funcionario {
  id: number;
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId: number;
  cargoDescricao: string;
  centroCustoId: number;
  centroCustoDescricao: string;
  linhaNegocioId: number;
  linhaNegocioDescricao: string;
  idExterno?: string;
  ativo: boolean;
}

export interface Rubrica {
  id: number;
  codigo: string;
  descricao: string;
  tipo: 'PROVENTO' | 'DESCONTO' | 'INFORMATIVO';
  porcentagem?: number;
  ativo: boolean;
}

export interface Beneficio {
  id: number;
  funcionarioId: number;
  funcionarioNome: string;
  descricao: string;
  valor: number;
  dataInicio: string;
  dataFim: string | null;
  observacao: string | null;
}

export interface FolhaPagamento {
  id: number;
  funcionarioId: number;
  funcionarioNome: string;
  rubricaId: number;
  rubricaCodigo: string;
  rubricaDescricao: string;
  rubricaTipo: string;
  dataInicio: string;
  dataFim: string;
  valor: number;
  quantidade: number;
  baseCalculo: number;
  ativo: boolean;
}

export interface LoginRequest {
  login: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: number;
    login: string;
    nome: string;
    permissoes: string[];
  };
}

export interface Cargo {
  id: number;
  descricao: string;
  ativo: boolean;
}

export interface CentroCusto {
  id: number;
  descricao: string;
  ativo: boolean;
  linhaNegocioId: number;
}

export interface LinhaNegocio {
  id: number;
  descricao: string;
  ativo: boolean;
}

export interface ImportacaoResponse {
  success: boolean;
  message: string;
  arquivo?: string;
  tamanho?: number;
  registrosProcessados?: number;
  erros?: string[];
} 