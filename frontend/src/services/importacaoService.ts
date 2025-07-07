import api from './api';
import type { ImportacaoResponse } from '../types';

const importacaoService = {
  importarFolha: async (arquivo: File): Promise<ImportacaoResponse> => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    
    const response = await api.post('/importacao/folha', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    return response.data;
  },

  importarFolhaAdp: async (arquivo: File): Promise<ImportacaoResponse> => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    
    const response = await api.post('/importacao/folha-adp', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
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
    });
    
    return response.data;
  }
};

export { importacaoService }; 