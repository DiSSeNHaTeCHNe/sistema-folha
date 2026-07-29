import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TokenService } from './tokenService';
import type { TokenData } from '../types';

const sampleTokenData = (): TokenData => ({
  token: 'access-token',
  refreshToken: 'refresh-token',
  tokenExpiration: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
  refreshExpiration: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
});

describe('TokenService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('isTokenExpired returns true when expiration is missing', () => {
    expect(TokenService.isTokenExpired()).toBe(true);
  });

  it('isTokenExpired returns true when less than five minutes remain', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      tokenExpiration: new Date(Date.now() + 2 * 60 * 1000).toISOString(),
    });

    expect(TokenService.isTokenExpired()).toBe(true);
  });

  it('isTokenExpired returns false when expiration is far in the future', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      tokenExpiration: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
    });

    expect(TokenService.isTokenExpired()).toBe(false);
  });

  it('clearTokens removes all stored auth keys', () => {
    TokenService.setTokens(sampleTokenData());

    TokenService.clearTokens();

    expect(TokenService.getToken()).toBeNull();
    expect(TokenService.getRefreshToken()).toBeNull();
    expect(TokenService.getTokenExpiration()).toBeNull();
    expect(TokenService.getRefreshExpiration()).toBeNull();
    expect(TokenService.getTokenData()).toBeNull();
  });

  it('setTokens and getTokenData round-trip localStorage values', () => {
    const tokens = sampleTokenData();

    TokenService.setTokens(tokens);

    expect(TokenService.getToken()).toBe(tokens.token);
    expect(TokenService.getRefreshToken()).toBe(tokens.refreshToken);
    expect(TokenService.getTokenData()).toEqual(tokens);
  });
});
