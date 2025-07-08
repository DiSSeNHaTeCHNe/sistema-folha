import axios from 'axios';
import type { LoginRequest, LoginResponse } from '../types';

// @ts-ignore
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8083/api',
});

api.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>('/auth/login', data);
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

export const getUserByLogin = async (login: string) => {
  const response = await api.get(`/usuarios/login/${login}`);
  return response.data;
};

export default api; 