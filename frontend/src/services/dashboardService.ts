import api from './api';

export interface DashboardStats {
  totalFuncionarios: number;
  custoMensalFolha: number;
  totalBeneficiosAtivos: number;
  porLinhaNegocio: LinhaNegocioStats[];
  porCentroCusto: CentroCustoStats[];
  porCargo: CargoStats[];
  totalProventos: number;
  totalDescontos: number;
  topProventos: RubricaStats[];
  topDescontos: RubricaStats[];
  evolucaoMensal: EvolucaoMensal[];
}

export interface LinhaNegocioStats {
  id: number;
  descricao: string;
  quantidadeFuncionarios: number;
  valorTotal: number;
}

export interface CentroCustoStats {
  id: number;
  descricao: string;
  quantidadeFuncionarios: number;
  valorTotal: number;
}

export interface CargoStats {
  id: number;
  descricao: string;
  quantidadeFuncionarios: number;
  valorMedio: number;
  valorTotal: number;
}

export interface RubricaStats {
  id: number;
  codigo: string;
  descricao: string;
  valorTotal: number;
  quantidadeOcorrencias: number;
}

export interface EvolucaoMensal {
  mesAno: string;
  valorTotal: number;
  quantidadeFuncionarios: number;
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = await api.get<DashboardStats>('/dashboard/stats');
  return response.data;
}; 