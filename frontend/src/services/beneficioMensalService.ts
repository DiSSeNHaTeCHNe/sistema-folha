import api from './api';
import type {
  BeneficioMensal,
  BeneficioMensalCompetenciaParams,
  BeneficioMensalCreateDTO,
  BeneficioMensalResumo,
  ImportacaoBeneficioMensalResultado,
} from '../types';

export const beneficioMensalService = {
  listar: async (params: BeneficioMensalCompetenciaParams): Promise<BeneficioMensal[]> => {
    const response = await api.get<BeneficioMensal[]>('/beneficio-mensal', { params });
    return response.data;
  },

  resumo: async (params: BeneficioMensalCompetenciaParams): Promise<BeneficioMensalResumo[]> => {
    const response = await api.get<BeneficioMensalResumo[]>('/beneficio-mensal/resumo', { params });
    return response.data;
  },

  porFuncionario: async (
    id: number,
    params: BeneficioMensalCompetenciaParams,
  ): Promise<BeneficioMensal[]> => {
    const response = await api.get<BeneficioMensal[]>(`/beneficio-mensal/funcionario/${id}`, { params });
    return response.data;
  },

  criar: async (dto: BeneficioMensalCreateDTO): Promise<BeneficioMensal> => {
    const response = await api.post<BeneficioMensal>('/beneficio-mensal', dto);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/beneficio-mensal/${id}`);
  },

  importar: async (
    file: File,
    competenciaInicio: string,
    competenciaFim: string,
    confirmar: boolean = false,
  ): Promise<ImportacaoBeneficioMensalResultado> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<ImportacaoBeneficioMensalResultado>(
      '/importacao/beneficios-mensais',
      formData,
      {
        params: { competenciaInicio, competenciaFim, confirmar },
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        timeout: 300000,
      },
    );

    return response.data;
  },
};
