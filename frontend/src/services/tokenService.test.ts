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

  it('getTokenExpiration returns null when not stored', () => {
    expect(TokenService.getTokenExpiration()).toBeNull();
  });

  it('getTokenExpiration parses stored ISO date', () => {
    const expiration = '2026-12-31T23:59:59.000Z';
    TokenService.setTokens({ ...sampleTokenData(), tokenExpiration: expiration });

    expect(TokenService.getTokenExpiration()?.toISOString()).toBe(expiration);
  });

  it('isRefreshTokenExpired returns true when refresh expiration is missing', () => {
    expect(TokenService.isRefreshTokenExpired()).toBe(true);
  });

  it('isRefreshTokenExpired returns true when refresh expiration is in the past', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      refreshExpiration: new Date(Date.now() - 60_000).toISOString(),
    });

    expect(TokenService.isRefreshTokenExpired()).toBe(true);
  });

  it('isRefreshTokenExpired returns false when refresh expiration is in the future', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      refreshExpiration: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    });

    expect(TokenService.isRefreshTokenExpired()).toBe(false);
  });

  it('hasValidTokens returns false when either token is missing', () => {
    localStorage.setItem('token', 'access-only');

    expect(TokenService.hasValidTokens()).toBe(false);
  });

  it('hasValidTokens returns false when refresh token is expired', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      refreshExpiration: new Date(Date.now() - 60_000).toISOString(),
    });

    expect(TokenService.hasValidTokens()).toBe(false);
  });

  it('hasValidTokens returns true when both tokens exist and refresh is valid', () => {
    TokenService.setTokens(sampleTokenData());

    expect(TokenService.hasValidTokens()).toBe(true);
  });

  it('needsRefresh returns true when access token is near expiry but refresh is valid', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      tokenExpiration: new Date(Date.now() + 2 * 60 * 1000).toISOString(),
      refreshExpiration: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    });

    expect(TokenService.needsRefresh()).toBe(true);
  });

  it('needsRefresh returns false when access token is still valid', () => {
    TokenService.setTokens({
      ...sampleTokenData(),
      tokenExpiration: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
    });

    expect(TokenService.needsRefresh()).toBe(false);
  });
});
