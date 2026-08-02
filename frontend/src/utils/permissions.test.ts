import { describe, expect, it } from 'vitest';
import { canAccessApiKeysPage, canCreateApiKey, isAdmin } from './permissions';
import type { Usuario } from '../types';

const adminUser: Usuario = { id: 1, login: 'admin', nome: 'Admin', permissoes: ['ADMIN'] };
const apiKeyUser: Usuario = { id: 2, login: 'api', nome: 'API', permissoes: ['API_KEY'] };
const regularUser: Usuario = { id: 3, login: 'user', nome: 'User', permissoes: ['CONSULTA'] };

describe('permissions utils', () => {
  it('detects admin users', () => {
    expect(isAdmin(adminUser)).toBe(true);
    expect(isAdmin(regularUser)).toBe(false);
    expect(isAdmin(null)).toBe(false);
    expect(isAdmin(undefined)).toBe(false);
  });

  it('allows API keys page for API_KEY or ADMIN', () => {
    expect(canAccessApiKeysPage(apiKeyUser)).toBe(true);
    expect(canAccessApiKeysPage(adminUser)).toBe(true);
    expect(canAccessApiKeysPage(regularUser)).toBe(false);
    expect(canAccessApiKeysPage(null)).toBe(false);
    expect(canAccessApiKeysPage({ ...regularUser, permissoes: undefined as unknown as string[] })).toBe(false);
  });

  it('allows API key creation only with API_KEY permission', () => {
    expect(canCreateApiKey(apiKeyUser)).toBe(true);
    expect(canCreateApiKey(adminUser)).toBe(false);
    expect(canCreateApiKey(null)).toBe(false);
  });
});
