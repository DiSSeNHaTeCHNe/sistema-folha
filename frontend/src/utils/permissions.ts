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
