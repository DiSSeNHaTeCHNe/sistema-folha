import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { login as apiLogin, logout as apiLogout, getUserByLogin } from '../services/api';
import { TokenService } from '../services/tokenService';
import type { LoginRequest, Usuario } from '../types';

interface AuthContextData {
  user: Usuario | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextData>({} as AuthContextData);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    initializeAuth();
    
    // Escutar evento de logout automático do interceptor
    const handleAutoLogout = () => {
      console.log('AuthContext - Auto logout triggered');
      handleLogout();
    };
    
    window.addEventListener('auth:logout', handleAutoLogout);
    
    return () => {
      window.removeEventListener('auth:logout', handleAutoLogout);
    };
  }, []);

  const initializeAuth = async () => {
    try {
      const storedUser = localStorage.getItem('user');
      console.log('AuthContext - storedUser:', storedUser);
      
      // Verificar se temos tokens válidos
      if (!TokenService.hasValidTokens()) {
        console.log('AuthContext - No valid tokens, clearing auth');
        clearAuth();
        return;
      }
      
      // Se temos um usuário armazenado, usar os dados
      if (storedUser) {
        try {
          const parsedUser = JSON.parse(storedUser);
          console.log('AuthContext - parsedUser:', parsedUser);
          setUser(parsedUser);
        } catch (error) {
          console.error('AuthContext - Error parsing stored user:', error);
          clearAuth();
        }
      }
    } catch (error) {
      console.error('AuthContext - Error initializing auth:', error);
      clearAuth();
    } finally {
      setLoading(false);
    }
  };

  const clearAuth = () => {
    TokenService.clearTokens();
    localStorage.removeItem('user');
    setUser(null);
  };

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const login = async (data: LoginRequest) => {
    try {
      const response = await apiLogin(data);
      console.log('AuthContext - login response:', response);
      
      // Salvar tokens usando o TokenService
      TokenService.setTokens({
        token: response.token,
        refreshToken: response.refreshToken,
        tokenExpiration: response.tokenExpiration,
        refreshExpiration: response.refreshExpiration,
      });
      
      // Buscar dados completos do usuário
      const userData = await getUserByLogin(response.login);
      console.log('AuthContext - user data:', userData);
      localStorage.setItem('user', JSON.stringify(userData));
      setUser(userData as Usuario);
      navigate('/dashboard');
    } catch (error) {
      console.error('AuthContext - login error:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch (error) {
      console.error('AuthContext - logout error:', error);
    } finally {
      handleLogout();
    }
  };

  const isAuthenticated = user !== null && TokenService.hasValidTokens();

  return (
    <AuthContext.Provider value={{ 
      user, 
      loading, 
      login, 
      logout, 
      isAuthenticated 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
} 