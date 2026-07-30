import axios from 'axios';
import type { LoginRequest, LoginResponse } from '../types';
import { getApiBaseUrl } from '../lib/apiBaseUrl';
import { TokenService } from './tokenService';

interface RetryableRequestConfig {
  url?: string;
  headers?: Record<string, string>;
  _retry?: boolean;
}

interface AxiosLikeError {
  config?: RetryableRequestConfig;
  response?: { status?: number };
}

// @ts-expect-error axios default export lacks create in bundled type resolution
const api = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
});

// Flag para evitar múltiplas tentativas de refresh simultâneas
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (error?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

const isUnauthorizedStatus = (status?: number): boolean =>
  status === 401;

const logoutOnAuthFailure = (): void => {
  TokenService.clearTokens();
  window.dispatchEvent(new CustomEvent('auth:logout'));
};

const shouldRefreshToken = (
  axiosError: AxiosLikeError,
  originalRequest: RetryableRequestConfig | undefined,
  isRefreshRequest: boolean,
): originalRequest is RetryableRequestConfig =>
  isUnauthorizedStatus(axiosError.response?.status)
  && originalRequest != null
  && !originalRequest._retry
  && !isRefreshRequest;

async function refreshAccessToken(originalRequest: RetryableRequestConfig): Promise<unknown> {
  const refreshToken = TokenService.getRefreshToken();

  if (!refreshToken) {
    throw new Error('Refresh token não disponível');
  }

  if (TokenService.isRefreshTokenExpired()) {
    throw new Error('Refresh token expirado');
  }

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

  TokenService.setTokens(newTokenData);
  processQueue(null, newTokenData.token);

  if (originalRequest.headers) {
    originalRequest.headers.Authorization = `Bearer ${newTokenData.token}`;
  }

  return api(originalRequest);
}

// Interceptor de requisição para adicionar token de autorização
api.interceptors.request.use(
  (config: unknown) => {
    const requestConfig = config as RetryableRequestConfig;
    const token = TokenService.getToken();
    if (token && requestConfig.headers) {
      requestConfig.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: unknown) => {
    throw error;
  }
);

// Interceptor de resposta para lidar com refresh automático
api.interceptors.response.use(
  (response: unknown) => {
    return response;
  },
  async (error: unknown) => {
    const axiosError = error as AxiosLikeError;
    const originalRequest = axiosError.config;
    const isRefreshRequest = originalRequest?.url?.includes('/auth/refresh') || false;

    if (shouldRefreshToken(axiosError, originalRequest, isRefreshRequest)) {
      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest)).catch((err: unknown) => { throw err; });
      }

      isRefreshing = true;

      try {
        return await refreshAccessToken(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        logoutOnAuthFailure();
        throw refreshError;
      } finally {
        isRefreshing = false;
      }
    }

    if (isUnauthorizedStatus(axiosError.response?.status) && isRefreshRequest) {
      console.log('Refresh token inválido ou expirado, fazendo logout...');
      logoutOnAuthFailure();
    }

    throw error;
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

/** Resets interceptor module state — for Vitest only. */
export function resetApiAuthState(): void {
  isRefreshing = false;
  failedQueue = [];
}

export default api;
