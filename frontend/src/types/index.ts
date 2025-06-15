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
  cargo: string;
  centroCusto: string;
  linhaNegocio: string;
  idExterno: string;
  dataAdmissao: string;
  sexo: string;
  tipoSalario: string;
  funcao: string;
  depIrrf: number;
  depSalFamilia: number;
  vinculo: string;
}

export interface Rubrica {
  id: number;
  codigo: string;
  descricao: string;
  tipo: 'PROVENTO' | 'DESCONTO';
  porcentagem: number | null;
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
  dataInicio: string;
  dataFim: string;
  valor: number;
  quantidade: number;
  baseCalculo: number;
}

export interface LoginRequest {
  login: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  login: string;
} 