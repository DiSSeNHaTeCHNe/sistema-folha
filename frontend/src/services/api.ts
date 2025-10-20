import axios from 'axios';
import type { LoginRequest, LoginResponse } from '../types';
import { TokenService } from './tokenService';

// @ts-ignore
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8083/api',
  timeout: 10000,
});

// Flag para evitar múltiplas tentativas de refresh simultâneas
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: any) => void;
  reject: (error?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

// Interceptor de requisição para adicionar token de autorização
api.interceptors.request.use(
  (config: any) => {
    const token = TokenService.getToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: any) => {
    return Promise.reject(error);
  }
);

// Interceptor de resposta para lidar com refresh automático
api.interceptors.response.use(
  (response: any) => {
    return response;
  },
  async (error: any) => {
    const originalRequest = error.config;
    const isRefreshRequest = originalRequest?.url?.includes('/auth/refresh') || false;
    
    // Se o erro é 401 ou 403 (token expirado/inválido) e não é uma tentativa de refresh
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry && !isRefreshRequest) {
      // Marcar a requisição como tentativa de retry
      originalRequest._retry = true;
      
      // Se já estamos fazendo refresh, colocar na fila
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => {
          // Retry da requisição original com o novo token
          return api(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }
      
      isRefreshing = true;
      
      try {
        const refreshToken = TokenService.getRefreshToken();
        
        if (!refreshToken) {
          throw new Error('Refresh token não disponível');
        }
        
        // Verificar se o refresh token não está expirado
        if (TokenService.isRefreshTokenExpired()) {
          throw new Error('Refresh token expirado');
        }
        
        // Fazer o refresh do token usando fetch nativo
        const refreshResponse = await fetch(`${api.defaults.baseURL}/auth/refresh`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ refreshToken }),
        });
        
        if (!refreshResponse.ok) {
          throw new Error('Falha ao renovar token');
        }
        
        const refreshData = await refreshResponse.json() as LoginResponse;
        
        const newTokenData = {
          token: refreshData.token,
          refreshToken: refreshData.refreshToken,
          tokenExpiration: refreshData.tokenExpiration,
          refreshExpiration: refreshData.refreshExpiration,
        };
        
        // Salvar os novos tokens
        TokenService.setTokens(newTokenData);
        
        // Processar a fila de requisições pendentes
        processQueue(null, newTokenData.token);
        
        // Retry da requisição original
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newTokenData.token}`;
        }
        
        return api(originalRequest);
        
      } catch (refreshError) {
        // Se o refresh falhou, processar a fila com erro
        processQueue(refreshError, null);
        
        // Limpar tokens e redirecionar para login
        TokenService.clearTokens();
        
        // Disparar evento personalizado para o contexto de auth
        window.dispatchEvent(new CustomEvent('auth:logout'));
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    // Se o erro é 401/403 e é do próprio endpoint de refresh, fazer logout
    if ((error.response?.status === 401 || error.response?.status === 403) && isRefreshRequest) {
      console.log('Refresh token inválido ou expirado, fazendo logout...');
      TokenService.clearTokens();
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }
    
    return Promise.reject(error);
  }
);

export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>('/auth/login', data);
  return response.data;
};

export const refreshToken = async (refreshToken: string): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>('/auth/refresh', { refreshToken });
  return response.data;
};

export const logout = async () => {
  try {
    const refreshToken = TokenService.getRefreshToken();
    if (refreshToken) {
      await api.post('/auth/logout', { refreshToken });
    }
  } catch (error) {
    console.error('Erro ao fazer logout no servidor:', error);
  } finally {
    TokenService.clearTokens();
  }
};

export const getUserByLogin = async (login: string) => {
  const response = await api.get(`/usuarios/login/${login}`);
  return response.data;
};

export default api; 