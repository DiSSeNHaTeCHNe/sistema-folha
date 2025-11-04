import api from './api';
import type { ImportacaoResponse } from '../types';

const importacaoService = {
  importarFolhaAdp: async (arquivo: File, decimoTerceiro: boolean = false, confirmarSubstituicao: boolean = false): Promise<ImportacaoResponse> => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('decimoTerceiro', decimoTerceiro.toString());
    formData.append('confirmarSubstituicao', confirmarSubstituicao.toString());
    
    const response = await api.post('/importacao/folha-adp', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 300000, // 5 minutos para operações de importação
    });
    
    return response.data;
  },

  importarBeneficios: async (arquivo: File): Promise<ImportacaoResponse> => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    
    const response = await api.post('/importacao/beneficios', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 300000, // 5 minutos para operações de importação
    });
    
    return response.data;
  }
};

export { importacaoService }; 