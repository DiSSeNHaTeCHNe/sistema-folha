import type { Usuario } from '../types';

export const CADASTRO_ROUTES = [
  '/usuarios',
  '/linhas-negocio',
  '/centros-custo',
  '/cargos',
  '/rubricas',
  '/rubricas-fixas',
  '/tipos-beneficio',
  '/organograma',
  '/importacao',
] as const;

export function isAdmin(user: Usuario | null | undefined): boolean {
  return user?.permissoes?.includes('ADMIN') ?? false;
}

export function canAccessApiKeysPage(user: Usuario | null | undefined): boolean {
  if (!user?.permissoes) {
    return false;
  }
  return user.permissoes.includes('API_KEY') || user.permissoes.includes('ADMIN');
}
