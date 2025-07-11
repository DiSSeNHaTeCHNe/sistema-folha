import type { TokenData } from '../types';

const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TOKEN_EXPIRATION_KEY = 'tokenExpiration';
const REFRESH_EXPIRATION_KEY = 'refreshExpiration';

export class TokenService {
  static setTokens(tokenData: TokenData): void {
    localStorage.setItem(TOKEN_KEY, tokenData.token);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenData.refreshToken);
    localStorage.setItem(TOKEN_EXPIRATION_KEY, tokenData.tokenExpiration);
    localStorage.setItem(REFRESH_EXPIRATION_KEY, tokenData.refreshExpiration);
  }

  static getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  static getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  static getTokenExpiration(): Date | null {
    const expiration = localStorage.getItem(TOKEN_EXPIRATION_KEY);
    return expiration ? new Date(expiration) : null;
  }

  static getRefreshExpiration(): Date | null {
    const expiration = localStorage.getItem(REFRESH_EXPIRATION_KEY);
    return expiration ? new Date(expiration) : null;
  }

  static isTokenExpired(): boolean {
    const expiration = this.getTokenExpiration();
    if (!expiration) return true;
    
    // Considera expirado se resta menos de 5 minutos
    const now = new Date();
    const fiveMinutes = 5 * 60 * 1000;
    return expiration.getTime() - now.getTime() < fiveMinutes;
  }

  static isRefreshTokenExpired(): boolean {
    const expiration = this.getRefreshExpiration();
    if (!expiration) return true;
    
    const now = new Date();
    return expiration.getTime() < now.getTime();
  }

  static hasValidTokens(): boolean {
    const token = this.getToken();
    const refreshToken = this.getRefreshToken();
    
    if (!token || !refreshToken) return false;
    
    // Se o refresh token está expirado, não temos tokens válidos
    if (this.isRefreshTokenExpired()) return false;
    
    return true;
  }

  static needsRefresh(): boolean {
    return this.hasValidTokens() && this.isTokenExpired();
  }

  static clearTokens(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXPIRATION_KEY);
    localStorage.removeItem(REFRESH_EXPIRATION_KEY);
  }

  static getTokenData(): TokenData | null {
    const token = this.getToken();
    const refreshToken = this.getRefreshToken();
    const tokenExpiration = localStorage.getItem(TOKEN_EXPIRATION_KEY);
    const refreshExpiration = localStorage.getItem(REFRESH_EXPIRATION_KEY);

    if (!token || !refreshToken || !tokenExpiration || !refreshExpiration) {
      return null;
    }

    return {
      token,
      refreshToken,
      tokenExpiration,
      refreshExpiration,
    };
  }
}

export default TokenService; 